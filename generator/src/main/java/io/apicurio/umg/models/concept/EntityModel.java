package io.apicurio.umg.models.concept;

import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.models.spec.SpecificationModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.*;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class EntityModel implements HasNamespacedName, HasProperties {

    @EqualsAndHashCode.Include
    private final NamespacedName nn;

    private final SpecificationModel specModel;

    private final Collection<TraitModel> traits = new LinkedHashSet<>();

    private final boolean root;

    private final List<String> propertyOrder;

    private final SpecificationVersion specVersion;

    private final Map<String, PropertyModel> properties = new LinkedHashMap<>();

    private boolean leaf;

    private EntityModel parent;

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
