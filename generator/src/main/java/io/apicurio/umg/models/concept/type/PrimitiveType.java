package io.apicurio.umg.models.concept.type;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.models.concept.typelike.TypeLike;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

import static io.apicurio.umg.logging.Errors.fail;

@Getter
@ToString
public enum PrimitiveType implements Type {

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
    public String getNamespace() {
        //throw new UnsupportedOperationException();
        return null;
    }

    @Override
    public Type getParent() {
        return null;
    }

    @Override
    public void setParent(TypeLike parent) {
        // TODO
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void setLeaf(boolean leaf) {
        fail("Primitive type cannot be leaf.");
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void setRoot(boolean root) {
        fail("Primitive type cannot be root.");
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public PrimitiveType copy() {
        return this;
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }
}
