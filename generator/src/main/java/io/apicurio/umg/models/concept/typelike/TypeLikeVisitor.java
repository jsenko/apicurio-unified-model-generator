package io.apicurio.umg.models.concept.typelike;

import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.concept.typelike.TraitTypeLike;

import static io.apicurio.umg.logging.Errors.fail;

public interface TypeLikeVisitor {

    default void visit(PrimitiveType type) {
    }

    default void visit(UnionType type) {
    }

    default void visit(ListType type) {
    }

    default void visit(MapType type) {
    }

    default void visit(EntityType type) {
    }

    default void visit(TraitTypeLike type) {
    }

    default void visit(Type type) {
        if (type instanceof PrimitiveType) {
            visit((PrimitiveType) type);
        } else if (type instanceof UnionType) {
            visit((UnionType) type);
        } else if (type instanceof ListType) {
            visit((ListType) type);
        } else if (type instanceof MapType) {
            visit((MapType) type);
        } else if (type instanceof EntityType) {
            visit((EntityType) type);
        } else if (type instanceof TraitTypeLike) {
            visit((TraitTypeLike) type);
        } else {
            fail("Unknown type-like: %s", type.getClass().getSimpleName());
        }
    }
}
