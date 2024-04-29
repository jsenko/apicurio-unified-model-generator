package io.apicurio.umg.models.concept.type;

import io.apicurio.umg.models.concept.typelike.AbstractTypeLike;
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
public abstract class AbstractType extends AbstractTypeLike implements Type {

    protected RawType rawType;

    protected boolean root;

    public Type getParent() {
        return (Type) parent;
    }

    @Override
    public boolean isType() {
        return true;
    }
}
