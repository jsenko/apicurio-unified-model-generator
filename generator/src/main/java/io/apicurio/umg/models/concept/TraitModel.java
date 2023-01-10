package io.apicurio.umg.models.concept;

import io.apicurio.umg.beans.SpecificationVersion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class TraitModel implements HasNamespacedName, HasProperties {

    @EqualsAndHashCode.Include
    private NamespacedName nn;

    private final String name;

    private final SpecificationVersion specVersion;

    private final Map<String, PropertyModel> properties = new LinkedHashMap<>();

    private boolean leaf;

    private TraitModel parent;

    private boolean transparent;

    public void addProperty(PropertyModel property) {
        this.properties.put(property.getName(), property);
    }

    public boolean hasProperty(String propertyName) {
        return this.properties.containsKey(propertyName);
    }

    public void removeProperty(String propertyName) {
        this.properties.remove(propertyName);
    }
}
