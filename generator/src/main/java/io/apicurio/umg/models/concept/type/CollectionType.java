package io.apicurio.umg.models.concept.type;

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
public abstract class CollectionType extends AbstractType {

    protected Type valueType;


    @Override
    public boolean isCollectionType() {
        return true;
    }
}
