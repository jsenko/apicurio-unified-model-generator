package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.beans.Property;
import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyType;
import io.apicurio.umg.models.concept.TraitModel;
import io.apicurio.umg.pipe.AbstractStage;

import java.util.HashMap;
import java.util.Map;

public class CreatePropertyModelsStage extends AbstractStage {

    private Map<String, PropertyModel> typeAliases;


    @Override
    protected void doProcess() {
        info("-- Creating Property Models --");

        typeAliases = new HashMap<>();

        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVersion -> {
            // Create property models for traits
            specVersion.getTraits().forEach(trait -> {
                String fqTraitName = specVersion.getNamespace() + "." + trait.getName();
                TraitModel traitModel = getState().getConceptIndex().lookupTrait(fqTraitName);
                trait.getProperties().forEach(property -> {
                    PropertyModel propertyModel = processProperty(specVersion, property);
                    info("Created trait property model: %s/%s", traitModel.fullyQualifiedName(), propertyModel.getName());
                    traitModel.getProperties().put(property.getName(), propertyModel);
                });
            });

            // Create property models for entities
            specVersion.getEntities().forEach(entity -> {
                String fqEntityName = specVersion.getNamespace() + "." + entity.getName();
                EntityModel entityModel = getState().getConceptIndex().lookupEntity(fqEntityName);
                entity.getProperties().forEach(property -> {
                    PropertyModel propertyModel = processProperty(specVersion, property);
                    info("Created entity property model: %s/%s", entityModel.fullyQualifiedName(), propertyModel.getName());
                    entityModel.getProperties().put(property.getName(), propertyModel);
                });
            });
        });
    }


    private PropertyModel processProperty(SpecificationVersion specVersion, Property property) {
        PropertyType type = PropertyType.parse(property.getType());
        Property aliasProperty = null;
        if(type.isSimple()) {
            aliasProperty = getState().getSpecIndex().getTypeAliasIndex().get(specVersion.getNamespace() + "." + type.getSimpleType());
        }
        if (aliasProperty != null) {
            return PropertyModel.builder()
                    .name(property.getName())
                    .collection(aliasProperty.getCollection() != null ? aliasProperty.getCollection() : property.getCollection())
                    .discriminator(aliasProperty.getDiscriminator() != null ? aliasProperty.getDiscriminator() : property.getDiscriminator())
                    .unionRules(aliasProperty.getUnionRules() != null ? aliasProperty.getUnionRules() : property.getUnionRules())
                    .rawType(aliasProperty.getName())
                    .type(PropertyType.parse(aliasProperty.getType()))
                    .aliasedOriginalRawType(property.getType())
                    .aliasedOriginalType(type)
                    .build();
        } else {
            return PropertyModel.builder()
                    .name(property.getName())
                    .collection(property.getCollection())
                    .discriminator(property.getDiscriminator())
                    .unionRules(property.getUnionRules())
                    .rawType(property.getType())
                    .type(type)
                    .build();
        }
    }
}
