package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents union type
 */
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EntityType extends AbstractType {

    @ToString.Exclude
    private EntityModel entity;

    @Override
    public EntityType copy() {
        return EntityType.builder()
                .namespace(namespace)
                .name(name)
                .rawType(rawType)
                .entity(entity)
                .parent(parent)
                .leaf(leaf)
                .root(root)
                .build();
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isEntityType() {
        return true;
    }

    public static EntityType fromEntity(EntityModel entity) {
        return EntityType.builder()
                .namespace(entity.getNamespace().fullName())
                .name(entity.getName())
                .rawType(RawType.parse(entity.getName()))
                .entity(entity)
                .build();
    }
}
