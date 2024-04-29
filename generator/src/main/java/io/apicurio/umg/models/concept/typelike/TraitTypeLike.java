package io.apicurio.umg.models.concept.typelike;

import io.apicurio.umg.models.concept.TraitModel;
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
public class TraitTypeLike extends AbstractTypeLike {

    @ToString.Exclude
    private TraitModel trait;

    @Override
    public TraitTypeLike copy() {
        return TraitTypeLike.builder()
                .namespace(namespace)
                .name(name)
                .trait(trait)
                .parent(parent)
                .leaf(leaf)
                .build();
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isTraitTypeLike() {
        return true;
    }

    public static TraitTypeLike fromTrait(TraitModel trait) {
        return TraitTypeLike.builder()
                .namespace(trait.getNamespace().fullName())
                .name(trait.getName())
                .trait(trait)
                .build();
    }
}
