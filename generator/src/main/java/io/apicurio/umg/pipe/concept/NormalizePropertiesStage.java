package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.index.concept.ConceptIndex;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.EntityOrTraitModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.TraitModel;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import io.apicurio.umg.pipe.AbstractStage;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NormalizePropertiesStage extends AbstractStage {
    @Override
    protected void doProcess() {

        // Normalize the Entity Properties
        normalize(
                getState().getConceptIndex().findEntities("").stream().filter(model -> !model.isLeaf()).collect(Collectors.toList()),
                parent -> (Collection<EntityOrTraitModel>) (Collection<?>) getState().findChildEntitiesFor((EntityModel) parent)
        );
        normalize(
                getState().getConceptIndex().findTraits("").stream().filter(model -> !model.isLeaf()).collect(Collectors.toList()),
                parent -> (Collection<EntityOrTraitModel>) (Collection<?>) getState().findChildTraitsFor((TraitModel) parent)
        );

        System.err.println();
    }


    public void normalize(List<EntityOrTraitModel> nonLeafs, Function<EntityOrTraitModel, Collection<EntityOrTraitModel>> findChildren) {
        int changesMade;
        do {
            changesMade = 0;
            for (EntityOrTraitModel parent : nonLeafs) {
                // Get all direct children of this parent entity.
                var children = findChildren.apply(parent);
                // Get a collection of all non-shadowed properties for the children
                var allProperties = new HashSet<PropertyModel>();
                children.forEach(child -> allProperties.addAll(child.getProperties().values().stream().filter(p -> !p.isShadowed()).collect(Collectors.toSet())));

                // Filter the full list of properties - only keep the properties that exist in *all* children.
                List<PropertyModel> propertiesToLift = allProperties.stream()
                        .filter(property -> children.stream()
                                .map(c -> hasPropertyWithRawType(c.getProperties(), property))
                                .reduce(true, (sub, element) -> sub && element))
                        .collect(Collectors.toList());

                // Now pull up each of the properties in the above list
                propertiesToLift.forEach(property -> {
                    // Make a copy, so we do not accidentally flip the shadowed field
                    var p = property.copy();
                    // Lift the property type
                    var v = new LiftingTypeLikeVisitor(parent.getNamespace().fullName(), getState().getConceptIndex());
                    p.getType().accept(v);
                    var t = v.getLast();
                    p.setType(t);
                    if(!parent.hasProperty(property.getName())) {
                        parent.addProperty(p);
                        // Mark as shadowed
                        children.forEach(child -> child.getProperties().get(property.getName()).setShadowed(true));
                    }
                });

                // Did we find any properties to lift?  If yes, increment "changes made".  We're going to keep
                // going through our model hierarchy until we've pulled up all the shared properties we can.
                changesMade += propertiesToLift.size();
            }
        } while (changesMade > 0);
    }


    /**
     * Checks if the given collection of properties contains the given property.  It must have a property with
     * the same name and the property must have the same raw type.
     *
     * @param properties
     * @param property
     */
    private boolean hasPropertyWithRawType(Map<String, PropertyModel> properties, PropertyModel property) {
        PropertyModel otherProperty = properties.get(property.getName());
        return otherProperty != null && otherProperty.getType().getRawType().equals(property.getType().getRawType());
    }


    /**
     * This visitor walks the provided type and any nested types,
     * and for each type it makes a copy into the target namespace (if the type already does not exist there).
     * In addition, it sets the copied type as a parent of the visited type.
     * <p>
     * The visitor walks the nested types depth-first, so when a visit method is executed, the nested types have already been visited.
     */
    private class LiftingTypeLikeVisitor implements TypeLikeVisitor {

        private String namespace;
        private ConceptIndex index;
        @Getter
        private Type last;

        public LiftingTypeLikeVisitor(String namespace, ConceptIndex index) {
            this.namespace = namespace;
            this.index = index;
        }

        @Override
        public void visit(PrimitiveType type) {
            last = type; // Primitive types are singletons, noop
        }

        @Override
        public void visit(UnionType type) {
            last = index.lookupOrIndex(namespace, type.getName(), () -> {
                var types = type.getTypes().stream().map(t -> {
                    if (t.isPrimitiveType()) {
                        return t;
                    } else {
                        return index.requireType(namespace, t.getName());
                    }
                }).collect(Collectors.toList());
                var c = type.copy();
                c.setNamespace(namespace);
                c.setTypes(types);
                return c;
            });
            type.setParent(last);
            last.setLeaf(false);
        }

        @Override
        public void visit(ListType type) {
            last = index.lookupOrIndex(namespace, type.getName(), () -> {
                var c = type.copy();
                if (type.getValueType().isPrimitiveType()) {
                    c.setValueType(type.getValueType());
                } else {
                    c.setValueType(index.requireType(namespace, type.getValueType().getName()));
                }
                c.setNamespace(namespace);
                return c;
            });
            type.setParent(last);
            last.setLeaf(false);
        }

        @Override
        public void visit(MapType type) {
            last = index.lookupOrIndex(namespace, type.getName(), () -> {
                var c = type.copy();
                if (type.getKeyType().isPrimitiveType()) {
                    c.setKeyType(type.getKeyType());
                } else {
                    c.setKeyType(index.requireType(namespace, type.getKeyType().getName()));
                }
                if (type.getValueType().isPrimitiveType()) {
                    c.setValueType(type.getValueType());
                } else {
                    c.setValueType(index.requireType(namespace, type.getValueType().getName()));
                }
                c.setNamespace(namespace);
                return c;
            });
            type.setParent(last);
            last.setLeaf(false);
        }

        @Override
        public void visit(EntityType type) {
            // These should've already been lifted by NormalizeEntitiesStage
            last = index.lookupOrIndex(namespace, type.getName(), () -> {
                var c = type.copy();
                c.setNamespace(namespace);
                c.setEntity(index.lookupEntity(namespace, type.getEntity().getName()));
                return c;
            });
            type.setParent(last);
            last.setLeaf(false);
        }
    }
}
