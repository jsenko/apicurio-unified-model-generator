package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.typelike.TraitTypeLike;
import io.apicurio.umg.models.java.type.IJavaType;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * Creates the java interfaces for all entities.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateTraitOrEntityInterfacesStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        /*
        getState().getConceptIndex().findEntities("").forEach(entity -> {
            createEntityInterface(entity);
        });
        */
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isTraitTypeLike() || t.getTypeModel().isEntityType())
                .filter(t -> !t.getTypeModel().isTraitTypeLike() || !((TraitTypeLike)t.getTypeModel()).getTrait().isTransparent())
                .forEach(t -> {
                    createInterface(t);
                });
        System.err.println();
    }

    private void createInterface(IJavaType type) {
        //String _package = getJavaEntityInterfacePackage(type.getTypeModel().getEntity());
        //String name = getJavaEntityInterfaceName(type.getTypeModel().getEntity());

        JavaInterfaceSource entityInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(type.getPackageName())
                .setName(type.getName(true, false))
                .setPublic();

        type.setInterfaceSource(entityInterface);

        getState().getJavaIndex().index(entityInterface);

        // If the entity is a root, it must extend the RootNode interface
        //if (type.getTypeModel().getEntity().isRoot()) {
        //    String rootNodeFQN = getRootNodeInterfaceFQN();
        //    JavaInterfaceSource rootNodeInterfaceSource = getState().getJavaIndex().lookupInterface(rootNodeFQN);
        //    entityInterface.addImport(rootNodeInterfaceSource);
        //    entityInterface.addInterface(rootNodeInterfaceSource);
        //}
    }
}