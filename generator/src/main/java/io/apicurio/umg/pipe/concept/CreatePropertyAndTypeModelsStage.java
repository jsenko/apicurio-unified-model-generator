package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.beans.Property;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.RawType;
import io.apicurio.umg.models.concept.TraitModel;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.pipe.AbstractStage;

import java.util.stream.Collectors;

import static io.apicurio.umg.pipe.Util.copy;

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
                    info("Created trait property model: %s/%s", traitModel.getNn().fullyQualifiedName(), propertyModel.getName());
                    traitModel.getProperties().put(property.getName(), propertyModel);
                });
            });

            // Create property models for entities
            specVersion.getEntities().forEach(entity -> {
                String fqEntityName = specVersion.getNamespace() + "." + entity.getName();
                EntityModel entityModel = getState().getConceptIndex().lookupEntity(fqEntityName);
                entity.getProperties().forEach(property -> {
                    PropertyModel propertyModel = processProperty(namespace, property, false);
                    info("Created entity property model: %s/%s", entityModel.getNn().fullyQualifiedName(), propertyModel.getName());
                    entityModel.getProperties().put(property.getName(), propertyModel);
                });
            });
        });
    }


    private Type processPrimitiveType(String namespace, RawType rawType) {
        var name = rawType.asRawType();
        return getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            // Namespace is not actually used
            return PrimitiveType.getByRawType(name);
        });
    }


    private Type processEntityOrAliasType(String namespace, RawType rawType) {
        var name = rawType.asRawType();
        return getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var entity = getState().getConceptIndex().lookupEntity(namespace, name);
            assertion(entity != null, "Could not find entity for type: %s", rawType);
            return EntityType.builder()
                    .contextNamespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .entity(entity)
                    .build();
        });
    }


    private Type processListType(String namespace, Property property, RawType rawType) {
        assertion(rawType.getNested().size() == 1);
        var name = rawType.asRawType();
        return getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nested = rawType.getNested().get(0);
            var valueType = processAnyType(namespace, property, false, nested);
            return ListType.builder()
                    .contextNamespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .valueType(valueType)
                    .build();
        });
    }


    private Type processMapType(String namespace, Property property, RawType rawType) {
        assertion(rawType.getNested().size() == 1);
        var name = rawType.asRawType();
        return getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nested = rawType.getNested().get(0);
            var keyType = processPrimitiveType(namespace, RawType.parse("string"));
            var valueType = processAnyType(namespace, property, false, nested);
            return MapType.builder()
                    .contextNamespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .keyType(keyType)
                    .valueType(valueType)
                    .build();
        });
    }


    private Type processUnionType(String namespace, Property property, boolean isAlias, RawType rawType) {
        var name = isAlias ? property.getName() : rawType.asRawType();
        return getState().getConceptIndex().lookupOrIndex(namespace, name, () -> {
            var nestedTypes = rawType.getNested().stream().map(rt -> processAnyType(namespace, property, false, rt)).collect(Collectors.toList());
            var t = UnionType.builder()
                    .contextNamespace(namespace)
                    .name(name)
                    .rawType(rawType)
                    .types(nestedTypes)
                    .unionRules(copy(property.getUnionRules()))
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
