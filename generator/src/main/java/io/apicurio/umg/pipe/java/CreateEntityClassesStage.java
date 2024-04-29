package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.pipe.java.method.BodyBuilder;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * Creates the java interfaces for all entities.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateEntityClassesStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        /*
        getState().getConceptIndex().findEntities("").forEach(entity -> {
            createEntityInterface(entity);
        });
        */
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isEntityType())
                .map(t -> (EntityJavaType) t)
                .filter(t -> t.getTypeModel().getEntity().isLeaf())
                .forEach(this::createEntityClass);
        System.err.println();
    }

    private void createEntityClass(EntityJavaType type) {
        String _package = getJavaEntityClassPackage(type.getTypeModel().getEntity());
        String name = getJavaEntityClassName(type.getTypeModel().getEntity());

        JavaClassSource entityClass = Roaster.create(JavaClassSource.class)
                .setPackage(_package)
                .setName(name)
                .setPublic();

        JavaInterfaceSource entityInterface = getState().getJavaIndex().lookupInterface(getJavaEntityInterfaceFQN(type.getTypeModel().getEntity()));
        entityClass.addImport(entityInterface);
        entityClass.addInterface(entityInterface);
/*
        if (type.getTypeModel().getEntity().isRoot()) {
            // Root entities must extends RootNodeImpl
            JavaClassSource rootNodeImpl = getState().getJavaIndex().lookupClass(getRootNodeEntityClassFQN());
            entityClass.addImport(rootNodeImpl);
            entityClass.extendSuperType(rootNodeImpl);

            // Root entities need a default constructor that passes in the right model type
            JavaEnumSource modelTypeEnum = getState().getJavaIndex().lookupEnum(getModelTypeEnumFQN());
            entityClass.addImport(modelTypeEnum);

            MethodSource<JavaClassSource> entityConstructor = entityClass.addMethod().setPublic().setConstructor(true);
            String prefix = getPrefix(type.getTypeModel().getEntity().getNamespace());
            String modelType = prefixToModelType(prefix);
            BodyBuilder body = new BodyBuilder();
            body.addContext("modelType", modelType);
            body.append("super(ModelType.${modelType});");
            entityConstructor.setBody(body.toString());
        } else {
            // All impl classes extend NodeImpl
            JavaClassSource nodeImpl = getState().getJavaIndex().lookupClass(getNodeEntityClassFQN());
            entityClass.addImport(nodeImpl);
            entityClass.extendSuperType(nodeImpl);
        }
*/
        type.setClassSource(entityClass);
        getState().getJavaIndex().index(entityClass);
    }
}