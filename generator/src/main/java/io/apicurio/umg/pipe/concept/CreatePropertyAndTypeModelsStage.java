package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.beans.Property;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.RawType;
import io.apicurio.umg.models.concept.TraitModel;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.pipe.AbstractStage;

import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.Utils.copy;

/**
 * Go through all properties and create both a type model and a property model
 */
public class CreatePropertyAndTypeModelsStage extends AbstractStage {


    @Override
    protected void doProcess() {
        info("-- Creating Property Models --");

        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVersion -> {
            String namespace = specVersion.getNamespace();

            // Process union aliases first, since they might show up int traits and entities
            specVersion.getTypeAliases().forEach(typeAlias -> {
                processProperty(namespace, typeAlias, true);
                info("Processed alias: %s", typeAlias.getName());
            });

            // Create property models for traits
            specVersion.getTraits().forEach(trait -> {
                String fqTraitName = specVersion.getNamespace() + "." + trait.getName();
                TraitModel traitModel = getState().getConceptIndex().lookupTrait(fqTraitName);
                trait.getProperties().forEach(property -> {
                    PropertyModel propertyModel = processProperty(namespace, property, false);
                    info("Created trait property model: %s/%s", traitModel.fullyQualifiedName(), propertyModel.getName());
                    traitModel.getProperties().put(property.getName(), propertyModel);
                });
            });

            // Create property models for entities
            specVersion.getEntities().forEach(entity -> {
                String fqEntityName = specVersion.getNamespace() + "." + entity.getName();
                EntityModel entityModel = getState().getConceptIndex().lookupEntity(fqEntityName);
                entity.getProperties().forEach(property -> {
                    PropertyModel propertyModel = processProperty(namespace, property, false);
                    info("Created entity property model: %s/%s", entityModel.fullyQualifiedName(), propertyModel.getName());
                    entityModel.getProperties().put(property.getName(), propertyModel);
                });
            });

            // Process the root
            var rootTypeString = specVersion.getRoot().getType();
            assertion(rootTypeString != null, "Root type must be specified.");
            // Currently root type must be an alias or an entity.
            // Later we check that all the nested types are only entities, since the root type must implement Node.
            Property rootProperty = specVersion.getTypeAliases().stream().filter(p -> p.getName().equals(rootTypeString)).findAny().orElse(null);
            if (rootProperty != null) {
                var t = processAnyType(namespace, rootProperty, false, RawType.parse(rootTypeString));
                t.setRoot(true);
            }
            if (rootProperty == null && specVersion.getEntities().stream().anyMatch(e -> e.getName().equals(rootTypeString))) {
                // Check that there is n entity with this name, and create a fake property which won't be used
                rootProperty = new Property();
                rootProperty.setName("__dummy");
                var t = processAnyType(namespace, rootProperty, false, RawType.parse(rootTypeString));
                t.setRoot(true);
            }
            if (rootProperty == null) {
                fail("Root type must refer to an entity or a type alias, which must be an union of (nested) entities.");
            }
            // TODO: We might allow specifying an union type (with union rules) without requiring an alias
            System.err.println();
        });
    }


    private Type processPrimitiveType(String namespace, RawType rawType) {
        var name = rawType.asRawType();
        return (Type) getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            // Namespace is not actually used
            return PrimitiveType.getByRawType(name);
        });
    }


    private Type processEntityOrAliasType(String namespace, RawType rawType) {
        var name = rawType.asRawType();
        return (Type) getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var entity = getState().getConceptIndex().lookupEntity(namespace, name);
            assertion(entity != null, "Could not find entity for type: %s", rawType);
            return EntityType.builder()
                    .namespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .entity(entity)
                    .leaf(true) // Mark all as leaf by default, we will change it later
                    .build();
        });
    }


    private Type processListType(String namespace, Property property, RawType rawType) {
        assertion(rawType.getNested().size() == 1);
        var name = rawType.asRawType();
        return (Type) getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nested = rawType.getNested().get(0);
            var valueType = processAnyType(namespace, property, false, nested);
            return ListType.builder()
                    .namespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .valueType(valueType)
                    .leaf(true) // Mark all as leaf by default, we will change it later
                    .build();
        });
    }


    private Type processMapType(String namespace, Property property, RawType rawType) {
        assertion(rawType.getNested().size() == 1);
        var name = rawType.asRawType();
        return (Type) getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nested = rawType.getNested().get(0);
            var keyType = processPrimitiveType(namespace, RawType.parse("string"));
            var valueType = processAnyType(namespace, property, false, nested);
            return MapType.builder()
                    .namespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .keyType(keyType)
                    .valueType(valueType)
                    .leaf(true) // Mark all as leaf by default, we will change it later
                    .build();
        });
    }


    private Type processUnionType(String namespace, Property property, boolean isAlias, RawType rawType) {
        var name = isAlias ? property.getName() : rawType.asRawType();
        return (Type) getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nestedTypes = rawType.getNested().stream().map(rt -> processAnyType(namespace, property, false, rt)).collect(Collectors.toList());
            var t = UnionType.builder()
                    .namespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .types(nestedTypes)
                    .unionRules(copy(property.getUnionRules()))
                    .leaf(true) // Mark all as leaf by default, we will change it later
                    .build();
            property.getUnionRules().clear();
            return t;
        });
    }


    private Type processAnyType(String namespace, Property property, boolean isAlias, RawType rawType) {
        if (rawType.isPrimitiveType()) {
            assertion(!isAlias, "Alias is not supported for primitive type: %s", rawType);
            return processPrimitiveType(namespace, rawType);
        } else if (rawType.isEntityType()) {
            assertion(!isAlias, "Alias is not supported for entity type: %s", rawType);
            return processEntityOrAliasType(namespace, rawType);
        } else if (rawType.isList()) {
            assertion(!isAlias, "Alias is not supported for list type: %s", rawType);
            return processListType(namespace, property, rawType);
        } else if (rawType.isMap()) {
            assertion(!isAlias, "Alias is not supported for map type: %s", rawType);
            return processMapType(namespace, property, rawType);
        } else if (rawType.isUnion()) {
            return processUnionType(namespace, property, isAlias, rawType);
        } else {
            fail("Unknown kind of type: %s", rawType);
            return null;
        }
    }


    private PropertyModel processProperty(String namespace, Property property, boolean isAlias) {
        RawType rawType = RawType.parse(property.getType());
        var typeModel = processAnyType(namespace, property, isAlias, rawType);
        return PropertyModel.builder()
                .name(property.getName())
                .type(typeModel)
                .discriminator(property.getDiscriminator())
                .collection(property.getCollection())
                .build();
    }
}
