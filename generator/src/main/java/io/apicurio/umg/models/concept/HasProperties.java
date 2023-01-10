package io.apicurio.umg.models.concept;

public interface HasProperties extends HasNamespacedName {



    void addProperty(PropertyModel property);

    boolean hasProperty(String propertyName);

    void removeProperty(String propertyName);
}
