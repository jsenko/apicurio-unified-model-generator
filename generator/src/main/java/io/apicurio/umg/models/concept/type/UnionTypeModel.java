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

    // Union is something between an entity and a type...
    //@EqualsAndHashCode.Include
    //private final NamespacedName nn;

    @EqualsAndHashCode.Include
    private String name;

    private RawType rawType;

    private List<TypeModel> types;

    private List<UnionRule> unionRules;
}
