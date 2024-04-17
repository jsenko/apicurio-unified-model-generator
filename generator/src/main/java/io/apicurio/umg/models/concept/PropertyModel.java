package io.apicurio.umg.models.concept;

import io.apicurio.umg.models.concept.type.TypeModel;
import lombok.Builder;
import lombok.Data;

/**
 * Models a single property in an entity or trait.
 */
@Builder
@Data
public class PropertyModel {

    // private Set<HasProperties> origins; TODO?

    private String name;

    private TypeModel type;

    /**
     * If this is a regex property, define the name of a collection that will contain the properties that match the regex.
     */
    private String collection;

    private String discriminator; // TODO

    /*
    private String rawType;

    private String aliasedOriginalRawType;

    private List<UnionRule> unionRules;

    private PropertyType type;

    private PropertyType aliasedOriginalType;

    public UnionRule getRuleFor(String rawUnionSubtype) {
        if (unionRules != null) {
            return unionRules.stream().filter(rule -> rule.getUnionType().equals(rawUnionSubtype)).findFirst().orElse(null);
        }
        return null;
    }

     */

    public PropertyModel copy() {
        return PropertyModel.builder()
                .name(name)
                .type(type)
                .collection(collection)
                .discriminator(discriminator)
                .build();
    }
}
