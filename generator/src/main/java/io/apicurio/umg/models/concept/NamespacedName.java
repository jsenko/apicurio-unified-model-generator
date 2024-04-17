package io.apicurio.umg.models.concept;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class NamespacedName {

    private NamespaceModel namespace;

    private final String name;

    public String fullyQualifiedName() {
        return namespace.fullName() + "." + name;
    }

    public static NamespacedName nn(NamespaceModel namespace, String name) {
        return NamespacedName.builder().namespace(namespace).name(name).build();
    }
}
