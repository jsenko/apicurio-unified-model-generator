package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.NamespaceModel;
import io.apicurio.umg.models.concept.type.EntityType;
import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.pipe.AbstractStage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;

public class NormalizeEntitiesStage extends AbstractStage {

    @Override
    protected void doProcess() {
        // Process every entity model we've created thus far
        Queue<EntityModel> modelsToProcess = new ConcurrentLinkedQueue<>();
        modelsToProcess.addAll(getState().getConceptIndex().findEntities(""));
        Set<String> modelsProcessed = new HashSet<>();

        // Keep working until we've processed every model (including any new models we
        // might create during processing).
        while (!modelsToProcess.isEmpty()) {
            EntityModel entity = modelsToProcess.remove();
            if (modelsProcessed.contains(entity.fullyQualifiedName())) {
                continue;
            }

            // Check if we need to create a parent entity for this model in any parent scope
            NamespaceModel ancestorNamespaceModel = entity.getNamespace().getParent();
            while (ancestorNamespaceModel != null) {
                if (needsParentEntity(ancestorNamespaceModel, entity.getName())) {
                    EntityModel ancestorEntity = EntityModel.builder()
                            .name(entity.getName())
                            .parent(entity.getParent())
                            .namespace(ancestorNamespaceModel)
                            .build();
                    ancestorNamespaceModel.getEntities().put(ancestorEntity.getName(), ancestorEntity);
                    modelsToProcess.add(ancestorEntity);
                    getState().getConceptIndex().index(ancestorEntity);

                    Collection<EntityModel> childEntities = getState().findChildEntitiesFor(ancestorEntity);
                    // Make the new parent entity the actual parent of each child entity
                    childEntities.forEach(childEntity -> {
                        childEntity.setParent(ancestorEntity);

                        // Add the new entity to type index
                        // We need to set the parent, so look for type representing the current entity
                        var entityType = getState().getConceptIndex().getEntityTypes()
                                .filter(e -> e.getEntity().equals(childEntity))
                                .collect(Collectors.toList());
                        assertion(entityType.size() == 1);
                        var ancestorType = getState().getConceptIndex().lookupOrIndex(EntityType.fromEntity(ancestorEntity));
                        entityType.get(0).setParent(ancestorType);
                        ancestorType.setLeaf(false);
                        ancestorType.setRoot(entityType.get(0).isRoot());

                        // Skip processing this model if its turn comes up in the queue.
                        modelsProcessed.add(childEntity.fullyQualifiedName());
                    });

                    // break out of loop - no need to search further up the hierarchy
                    ancestorNamespaceModel = null;
                } else {
                    ancestorNamespaceModel = ancestorNamespaceModel.getParent();
                }
            }
        }
    }

    /**
     * A entity needs a parent entity if there are multiple entities with the same name in the
     * namespace hierarchy.
     *
     * @param namespaceModel
     * @param entityName
     */
    private boolean needsParentEntity(NamespaceModel namespaceModel, String entityName) {
        int count = 0;
        for (NamespaceModel childNamespace : namespaceModel.getChildren().values()) {
            if (childNamespace.containsEntity(entityName)) {
                count++;
            }
        }
        return count > 1;
    }

}
