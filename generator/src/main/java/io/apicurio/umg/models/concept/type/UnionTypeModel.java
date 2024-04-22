package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.beans.UnionRule;
import io.apicurio.umg.models.concept.RawType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents union type
 */
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UnionTypeModel implements /*HasNamespacedName,*/ TypeModel {

    @EqualsAndHashCode.Include
    private String contextNamespace;

    @EqualsAndHashCode.Include
    private String name;

    private RawType rawType;

    private List<TypeModel> types;

    private List<UnionRule> unionRules;

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
