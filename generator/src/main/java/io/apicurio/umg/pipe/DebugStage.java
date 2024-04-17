package io.apicurio.umg.pipe;

import java.util.stream.Collectors;

public class DebugStage extends AbstractStage {

    @Override
    protected void doProcess() {
        debug("--- DEBUG ---");
        getState().getConceptIndex().findNamespaces("io.apicurio").forEach(namespace -> {
            debug("==========================");
            debug("Namespace: " + namespace.fullName());
            debug("==========================");
            namespace.getTraits().values().forEach(trait -> {
                debug("    Trait: " + trait.getName());
                trait.getProperties().values().forEach(field -> {
                    debug("        Property: " + field.getName() + " (" + field.getType() + ")");
                });
            });
            namespace.getEntities().values().forEach(entity -> {
                debug("    Entity: " + entity.getNn().getName());
                entity.getTraits().forEach(trait -> {
                    debug("        Trait: " + trait.getName());
                });
                entity.getProperties().values().forEach(field -> {
                    debug("        Property: " + field.getName() + " (" + field.getType() + ")");
                });
            });
            if (namespace.getVisitor() != null) {
                debug("    Visitor (%d entities): " + namespace.getVisitor().getEntities().stream().map(entity -> entity.getNn().getName()).collect(Collectors.toList()),
                        namespace.getVisitor().getEntities().size());
            }
        });
        debug("---");
    }


}
