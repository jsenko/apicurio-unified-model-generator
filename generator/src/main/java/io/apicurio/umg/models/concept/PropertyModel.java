package io.apicurio.umg.models.concept;

import io.apicurio.umg.models.concept.type.Type;
import lombok.Builder;
import lombok.Data;

import static io.apicurio.umg.logging.Errors.assertion;

/**
 * Models a single property in an entity or trait.
 */
@Builder
@Data
public class PropertyModel {

    private String name;

    private String collection;

    private String discriminator;

    private Type type;

    /**
     * Is true if this property has been lifted during normalization.
     * In some cases when processing leaf entities, we want to have all properties available.
     * If this is not desirable, you can filter them out using this field.
     */
    private boolean shadowed;

    public PropertyModel copy() {
        return PropertyModel.builder()
                .name(name)
                .collection(collection)
                .discriminator(discriminator)
                .type(type)
                .shadowed(shadowed)
                .build();
    }

    public boolean isRegex() {
        return name.startsWith("/");
    }

    public boolean isStar() {
        return "*".equals(name);
    }

    public String getEffectiveName() {
        if (isStar()) {
            return "_items";
        } else if (isRegex()) {
            assertion(collection != null); // TODO
            return collection;
        } else {
            return name;
        }
    }
}
