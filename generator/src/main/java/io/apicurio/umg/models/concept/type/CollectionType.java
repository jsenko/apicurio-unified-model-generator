package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.RawType;
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
public abstract class CollectionType implements Type {

    @EqualsAndHashCode.Include
    private String contextNamespace;

    @EqualsAndHashCode.Include
    private String name;

    private RawType rawType;

    private Type valueType;

    @Override
    public boolean isCollectionType() {
        return true;
    }
}
