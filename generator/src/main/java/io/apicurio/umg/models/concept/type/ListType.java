package io.apicurio.umg.models.concept.type;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
public class ListType extends CollectionType {

    @Override
    public boolean isListType() {
        return true;
    }

    @Override
    public boolean isPrimitiveListType() {
        return getValueType().isPrimitiveType();
    }

    @Override
    public boolean isEntityListType() {
        return getValueType().isEntityType();
    }
}
