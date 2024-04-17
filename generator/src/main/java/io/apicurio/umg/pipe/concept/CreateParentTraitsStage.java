package io.apicurio.umg.pipe.concept;

import java.util.LinkedList;
import java.util.List;

import io.apicurio.umg.models.concept.*;
import org.apache.commons.lang3.StringUtils;

import io.apicurio.umg.pipe.AbstractStage;

/**
 * Creates "parent" traits for all entities with child properties.  Only creates the parent trait
 * if there are at least two entities that have that child as a property.  For example, the "Contact"
 * entity is always a child of the "Info" entity, so a "ContactParent" trait is not created as it
 * is not needed.
 * <p>
 * The purpose of the Parent trait is to have a common trait used to manage the parent-child
 * relationship of an entity during visitation.
 */
public class CreateParentTraitsStage extends AbstractStage {

    @Override
    protected void doProcess() {

        // We need to remove the properties we lifted into the parent trait, so we remember them here
        List<PropertyModelWithOrigin> propertiesToRemove = new LinkedList<>();

        getState().getConceptIndex().findEntities("").stream().filter(entity -> entity.isLeaf()).forEach(entity -> {
            // For each leaf entity, go through all properties, and select a combination where a parent trait is needed
            entity.getProperties().values().stream().filter(property -> needsParent(entity, property)).forEach(property -> {

                if (!property.getType().isListType() && !property.getType().isMapType()) {
                    String propertyTypeName = property.getType().getRawType().getSimpleType();
                    String traitName = propertyTypeName + "Parent";
                    TraitModel parentTrait;
                    if (entity.getNn().getNamespace().containsTrait(traitName)) {
                        parentTrait = entity.getNn().getNamespace().getTraits().get(traitName);
                    } else {
                        parentTrait = TraitModel.builder().nn(NamespacedName.nn(entity.getNn().getNamespace(), traitName)).build();
                        // Copy the property
                        var traitProperty = property.copy();
                        parentTrait.getProperties().put(property.getName(), traitProperty);
                        entity.getNn().getNamespace().getTraits().put(traitName, parentTrait);
                        getState().getConceptIndex().index(parentTrait);
                    }
                    entity.getTraits().add(parentTrait);

                    PropertyModelWithOrigin pmwo = PropertyModelWithOrigin.builder().property(property).origin(entity).build();
                    propertiesToRemove.add(pmwo);
                }
            });
        });

        propertiesToRemove.forEach(property -> {
            property.getOrigin().removeProperty(property.getProperty().getName());
        });
    }

    /**
     * Returns true if the given entity/property combination is not unique across the models.  If the
     * combination is unique, then no parent trait is needed.  If it is NOT unique (meaning there
     * are other entities with the same property) then a parent IS needed.
     *
     * In other words, we're trying to find multiple entities that share a child entity property.
     * So if "Foo" and "Bar" entities both have a property called "widget" of type "Widget", then
     * we want those entities to both have a "WigetParent" trait.
     *
     * @param entity
     * @param property
     */
    private boolean needsParent(EntityModel entity, PropertyModel property) {
        if (isStarProperty(property)) {
            return false;
        }
        if (isRegexProperty(property)) {
            return false;
        }
        if (!isEntity(property)) {
            return false;
        }
        String propertyName = property.getName();
        String propertyTypeName = property.getType().getRawType().getSimpleType();
        if (!propertyName.equals(StringUtils.capitalize(propertyTypeName))) {
            return false;
        }
        return getState().getConceptIndex().findEntities("")
                .stream()
                .filter(e -> !e.getNn().getName().equals(entity.getNn().getName()))
                .filter(e -> e.hasProperty(property.getName()))
                .filter(e -> e.getProperties().get(property.getName()).equals(property))
                .count() > 0;
    }

}
