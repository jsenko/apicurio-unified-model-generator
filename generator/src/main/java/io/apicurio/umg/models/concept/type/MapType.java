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
public class MapType extends CollectionType {

    private Type keyType; // TODO This is always a string for now

    @Override
    public boolean isMapType() {
        return true;
    }

    @Override
    public boolean isPrimitiveMapType() {
        return getValueType().isPrimitiveType();
    }
}
