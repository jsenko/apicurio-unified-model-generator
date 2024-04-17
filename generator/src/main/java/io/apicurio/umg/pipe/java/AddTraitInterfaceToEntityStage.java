package io.apicurio.umg.pipe.java;

import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import io.apicurio.umg.models.concept.EntityModel;

/**
 * Configures the trait interfaces on each entity interface.
 *
 * @author eric.wittmann@gmail.com
 */
public class AddTraitInterfaceToEntityStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        getState().getConceptIndex().findEntities("").forEach(entity -> {
            configureEntityTraits(entity);
        });
    }

    private void configureEntityTraits(EntityModel entity) {
        JavaInterfaceSource javaEntity = getState().getJavaIndex().lookupInterface(getJavaEntityInterfaceFQN(entity));
        entity.getTraits().forEach(trait -> {
            JavaInterfaceSource javaTrait = getState().getJavaIndex().lookupInterface(getJavaTraitInterfaceFQN(trait));
            javaEntity.addImport(javaTrait);
            javaEntity.addInterface(javaTrait);
        });
    }
}