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
public class ListType extends CollectionType {

    @Override
    public ListType copy() {
        return ListType.builder()
                .namespace(namespace)
                .name(name)
                .rawType(rawType)
                .valueType(valueType)
                .parent(parent)
                .leaf(leaf)
                .root(root)
                .build();
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        valueType.accept(visitor);
        visitor.visit(this);
    }

    @Override
    public boolean isListType() {
        return true;
    }

    @Override
    public boolean isPrimitiveListType() {
        return valueType.isPrimitiveType();
    }

    @Override
    public boolean isEntityListType() {
        return valueType.isEntityType();
    }
}
