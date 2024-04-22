package io.apicurio.umg.models.concept.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.models.concept.RawType;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Getter
@ToString
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
            rawTypeMap.put(type.name, type);
        }
    }

    public static PrimitiveType getByRawType(String rawType) {
        return rawTypeMap.get(rawType);
    }

    PrimitiveType(String name, Class<?> _class) {
        this.name = name;
        this.rawType = RawType.parse(name);
        this._class = _class;
    }

    private String name;

    private Class<?> _class;

    private RawType rawType;


    @Override
    public String getContextNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }
}
