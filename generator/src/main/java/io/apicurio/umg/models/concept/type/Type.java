package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.typelike.TypeLike;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;

/**
 * Represents a thing that can be a type of a property.
 * For example: entity, union, collection of another type.
 * Traits are not a type.
 */
public interface Type extends TypeLike {

    boolean isRoot();

    void setRoot(boolean root);

    RawType getRawType();

    /**
     * Get a type that has the same name, but is in a parent namespace.
     * May be null.
     */
    @Override
    Type getParent();



}
