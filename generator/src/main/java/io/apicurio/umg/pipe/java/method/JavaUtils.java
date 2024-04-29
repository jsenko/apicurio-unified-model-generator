package io.apicurio.umg.pipe.java.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.type.*;
import io.apicurio.umg.pipe.GeneratorState;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.modeshape.common.text.Inflector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.Utils.addIfNotNull;
import static java.util.Map.entry;

public class JavaUtils {

    private static final Inflector INFLECTOR = new Inflector();

    public static Map<String, Class<?>> PRIMITIVE_TYPE_MAP = Map.ofEntries(
            entry("string", String.class),
            entry("boolean", Boolean.class),
            entry("number", Number.class),
            entry("integer", Integer.class),
            entry("object", ObjectNode.class),
            entry("any", JsonNode.class)
    );

    public static Map<String, String> JAVA_KEYWORD_MAP = Map.ofEntries(
            entry("default", "_default"),
            entry("enum", "_enum"),
            entry("const", "_const"),
            entry("if", "_if"),
            entry("else", "_else")
    );

    public static String sanitizeFieldName(String name) {
        if (name == null) {
            return null;
        }
        return JAVA_KEYWORD_MAP.getOrDefault(name, name);
    }

    public static String singularize(String name) {
        return INFLECTOR.singularize(name);
    }

    public static Set<IJavaType> collectNestedJavaTypes(GeneratorState state, IJavaType type) {
        var res = new HashSet<IJavaType>();
        _collectNestedJavaTypes(state, type, res);
        return res;
    }

    private static void _collectNestedJavaTypes(GeneratorState state, IJavaType type, Set<IJavaType> result) {
        assertion(type != null);
        if (type instanceof UnionJavaType) {
            addIfNotNull(result, type);
            ((UnionJavaType) type).getTypeModel().getTypes().forEach(t -> {
                var nested = state.getJavaIndex().requireType(t);
                _collectNestedJavaTypes(state, nested, result);
            });
        } else if (type instanceof CollectionJavaType) {
            addIfNotNull(result, type);
            var nested = state.getJavaIndex().requireType(((CollectionJavaType) type).getTypeModel().getValueType());
            _collectNestedJavaTypes(state, nested, result);
        } else {
            addIfNotNull(result, type);
        }
    }

    public static boolean hasMethod(MethodHolderSource<?> source, String name) {
        for (MethodSource<?> method : source.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static IJavaType getOldestParent(GeneratorState state, IJavaType t) {
        var parent = t;
        while (parent.getTypeModel().getParent() != null) {
            parent = state.getJavaIndex().requireType(parent.getTypeModel().getParent());
        }
        return parent;
    }

    public static boolean isOldestParent(GeneratorState state, IJavaType t) {
        var parent = getOldestParent(state, t);
        return parent.equals(t);
    }

    public static Collection<PropertyModel> extractProperties(IJavaType javaType, boolean includeTraits) {
        if (javaType instanceof EntityJavaType) {
            var res = new HashSet<PropertyModel>();
            res.addAll(((EntityJavaType) javaType).getTypeModel().getEntity().getProperties().values());
            if (includeTraits) {
                ((EntityJavaType) javaType).getTypeModel().getEntity().getTraits().forEach(t -> res.addAll(t.getProperties().values()));
            }
            return res;
        } else if (javaType instanceof TraitJavaType) {
            return ((TraitJavaType) javaType).getTypeModel().getTrait().getProperties().values();
        } else {
            fail("TODO");
        }
        return null; // Unreachable
    }
}
