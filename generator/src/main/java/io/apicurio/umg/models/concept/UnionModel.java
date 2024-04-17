package io.apicurio.umg.models.concept;

import io.apicurio.umg.beans.UnionRule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
public class UnionModel implements HasNamespacedName, TypeModel {

    @EqualsAndHashCode.Include
    private final NamespacedName nn;


    private Set<TypeModel> types = new HashSet<>();

    private List<UnionRule> unionRules;

    private String rawTypeExpression;
}
