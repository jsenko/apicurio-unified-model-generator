package io.apicurio.umg.models.concept.typelike;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public abstract class AbstractTypeLike implements TypeLike {

    @EqualsAndHashCode.Include
    protected String namespace;

    @EqualsAndHashCode.Include
    protected String name;

    protected TypeLike parent;

    protected boolean leaf;
}
