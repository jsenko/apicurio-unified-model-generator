package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.beans.UnionRule;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import io.apicurio.umg.pipe.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Represents union type
 */
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(callSuper = true)
public class UnionType extends AbstractType {

    private List<Type> types;

    private List<UnionRule> unionRules;

    @Override
    public UnionType copy() {
        return UnionType.builder()
                .namespace(namespace)
                .name(name)
                .rawType(rawType)
                .types(Utils.copy(types))
                .unionRules(Utils.copy(unionRules))
                .parent(parent)
                .leaf(leaf)
                .root(root)
                .build();
    }

    @Override
    public void accept(TypeLikeVisitor visitor) {
        types.forEach(t -> t.accept(visitor));
        visitor.visit(this);
    }

    @Override
    public boolean isUnionType() {
        return true;
    }

    public UnionRule getRuleFor(String rawUnionSubtype) {
        if (unionRules != null) {
            return unionRules.stream().filter(rule -> rule.getUnionType().equals(rawUnionSubtype)).findFirst().orElse(null);
        }
        return null;
    }
}
