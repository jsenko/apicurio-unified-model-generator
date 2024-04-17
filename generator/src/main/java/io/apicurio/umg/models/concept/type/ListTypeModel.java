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
public class ListTypeModel implements TypeModel {

    @EqualsAndHashCode.Include
    private String name;

    private RawType rawType;

    private TypeModel valueType;

    @Override
    public boolean isListType() {
        return true;
    }
}
