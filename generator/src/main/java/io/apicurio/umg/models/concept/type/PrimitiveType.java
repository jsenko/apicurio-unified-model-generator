package io.apicurio.umg.models.concept.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.models.concept.RawType;

import java.util.HashMap;
import java.util.Map;

public enum PrimitiveType implements TypeModel {

    STRING("string", String.class),
    BOOLEAN("boolean", Boolean.class),
    NUMBER("number", Number.class),
    INTEGER("integer", Integer.class),
    OBJECT("object", ObjectNode.class),
    ANY("any", JsonNode.class);

    private static final Map<String, PrimitiveType> rawTypeMap = new HashMap<>();

    static {
        for (PrimitiveType type : PrimitiveType.values()) {
            rawTypeMap.put(type.rawType, type);
        }
    }

    public static PrimitiveType getByRawType(String rawType) {
        return rawTypeMap.get(rawType);
    }

    PrimitiveType(String rawType, Class<?> clazz) {
        this.rawType = rawType;
        this.clazz = clazz;
    }

    private String rawType;

    private Class<?> clazz;

    public String getName() {
        return rawType;
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }
}
