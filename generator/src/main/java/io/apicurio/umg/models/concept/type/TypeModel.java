package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.RawType;

/**
 * Represents a thing that can be a type of a property.
 * For example: entity, union, collection of another type.
 */
public interface TypeModel {

    String getName();

    RawType getRawType();

    default boolean isEntityType() {
        return false;
    }

    default boolean isPrimitiveType() {
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
}
