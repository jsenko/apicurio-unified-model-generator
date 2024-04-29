package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class MapType extends CollectionType {

    private Type keyType; // TODO: This is always a String (for now)

    @Override
    public MapType copy() {
        return MapType.builder()
                .namespace(namespace)
                .name(name)
                .rawType(rawType)
                .keyType(keyType)
                .valueType(valueType)
                .parent(parent)
                .leaf(leaf)
                .root(root)
                .build();
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        keyType.accept(visitor);
        valueType.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public boolean isMapType() {
        return true;
    }

    @Override
    public boolean isPrimitiveMapType() {
        return valueType.isPrimitiveType();
    }

    @Override
    public boolean isEntityMapType() {
        return valueType.isEntityType();
    }
}
