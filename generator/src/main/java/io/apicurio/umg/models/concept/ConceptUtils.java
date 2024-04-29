package io.apicurio.umg.models.concept;

import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.concept.typelike.TypeLike;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.pipe.Utils.addIfNotNull;

public class ConceptUtils {


    public static PropertyModel asStringMapOf(String name, PropertyModel value) {

        var mapRawType = RawType.builder()
                .nested(List.of(value.getType().getRawType()))
                .map(true)
                .build();

        var mapType = MapType.builder()
                .name(mapRawType.asRawType())
                .keyType(PrimitiveType.STRING)
                .valueType(value.getType())
                .rawType(mapRawType)
                .namespace(value.getType().getNamespace())
                .build();

        return PropertyModel.builder()
                .name(name)
                .type(mapType)
                .build();
    }


    public static Set<TypeLike> collectNestedTypes(TypeLike type) {
        var res = new HashSet<TypeLike>();
        _collectNestedTypes(type, res, false);
        return res;
    }

    public static Set<TypeLike> collectNestedTypesShallow(TypeLike type) {
        var res = new HashSet<TypeLike>();
        _collectNestedTypes(type, res, true);
        return res;
    }

    private static void _collectNestedTypes(TypeLike type, Set<TypeLike> result, boolean shallow) {
        assertion(type != null);
        if (type instanceof UnionType) {
            addIfNotNull(result, type);
            ((UnionType) type).getTypes().forEach(t -> {
                _collectNestedTypes(t, result, shallow);
            });
        } else if (type instanceof CollectionType) {
            addIfNotNull(result, type);
            if (!shallow) {
                _collectNestedTypes(((CollectionType) type).getValueType(), result, shallow);
            }
        } else {
            addIfNotNull(result, type);
        }
    }
}
