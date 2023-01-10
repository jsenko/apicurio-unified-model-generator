package io.apicurio.umg.pipe.java;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.apicurio.umg.models.concept.PropertyModelWithOrigin;

public abstract class AbstractUnionTypeJavaStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        Set<PropertyModelWithOrigin> unionProperties = new HashSet<>();
        getState().getConceptIndex().findEntities("").stream().filter(entity -> entity.isLeaf()).forEach(entity -> {
            // For each leaf entity
            // Collect all properties
            Collection<PropertyModelWithOrigin> allProperties = getState().getConceptIndex().getAllEntityProperties(entity);
            // Find properties that are unions
            unionProperties.addAll(allProperties.stream().filter(property -> isUnion(property.getProperty())).collect(Collectors.toSet()));
        });

        // i.e. given the entity, collect all union properties
        unionProperties.forEach(property -> {
            doProcess(property);
        });
    }

    protected abstract void doProcess(PropertyModelWithOrigin property);

}
