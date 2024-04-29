package io.apicurio.umg.models.concept.typelike;

/**
 * Represents a thing that is not a type of a property,
 * but gets converted to a *JavaType.
 * Currently, this is just traits.
 */
public interface TypeLike {

    String getNamespace();

    String getName();

    /**
     * Get a type that has the same name, but is in a parent namespace.
     * May be null.
     */
    TypeLike getParent();

    void setParent(TypeLike parent);

    boolean isLeaf();

    void setLeaf(boolean leaf);

    void accept(TypeLikeVisitor visitor);

    TypeLike copy();

    //void accept(TypeVisitor visitor);

    default boolean isType() {
        return false;
    }

    default boolean isTraitTypeLike() {
        return false;
    }

    default boolean isEntityType() {
        return false;
    }

    default boolean isPrimitiveType() {
        return false;
    }

    default boolean isPrimitiveListType() {
        return false;
    }

    default boolean isPrimitiveMapType() {
        return false;
    }

    default boolean isEntityListType() {
        return false;
    }

    default boolean isEntityMapType() {
        return false;
    }

    default boolean isListType() {
        return false;
    }

    default boolean isMapType() {
        return false;
    }

    default boolean isUnionType() {
        return false;
    }

    default boolean isCollectionType() {
        return false;
    }
}
