package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.RawType;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class EntityType implements Type {

    @EqualsAndHashCode.Include
    private String contextNamespace;

    @EqualsAndHashCode.Include
    private String name;

    private RawType rawType;

    @ToString.Exclude
    private EntityModel entity;

    @Override
    public boolean isEntityType() {
        return true;
    }
}
