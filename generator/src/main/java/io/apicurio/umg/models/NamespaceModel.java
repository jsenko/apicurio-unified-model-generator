package io.apicurio.umg.models;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class NamespaceModel {

    @Include
    private NamespaceModel parent;
    @Include
    private String name;
    private final Map<String, NamespaceModel> children = new HashMap<>();
    private final Map<String, EntityModel> entities = new HashMap<>();
    private final Map<String, TraitModel> traits = new HashMap<>();
    private VisitorModel visitor;

    public String fullName() {
        return (parent != null ? parent.fullName() + "." : "") + name;
    }

    public boolean containsEntity(String entityName) {
        return entities.containsKey(entityName);
    }

    public boolean containsTrait(String traitName) {
        return traits.containsKey(traitName);
    }

    @Override
    public String toString() {
        return "Namespace<" + fullName() + ">";
    }
}
