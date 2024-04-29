package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.pipe.java.method.BodyBuilder;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * Creates the "emptyClone" method for all entity implementations.  This is required by the
 * Node interface (all model nodes must implement it).
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateEmptyCloneMethodStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isEntityType())
                .map(t -> (EntityJavaType) t)
                .filter(t -> t.getTypeModel().getEntity().isLeaf())
                .forEach(this::createEmptyCloneMethod);
    }

    /**
     * Creates the "emptyClone" method, needed by the Node interface that all nodes must
     * implement.
     */
    private void createEmptyCloneMethod(EntityJavaType javaEntity) {
        String nodeFQN = getNodeEntityInterfaceFQN();
        JavaInterfaceSource nodeInterfaceSource = getState().getJavaIndex().lookupInterface(nodeFQN);

        BodyBuilder body = new BodyBuilder();
        body.addContext("implClassName", javaEntity.getName(true, true));
        body.append("return new ${implClassName}();");

        javaEntity.getClassSource().addMethod()
                .setName("emptyClone")
                .setReturnType(javaEntity.getInterfaceSource())
                .setBody(body.toString())
                .setPublic()
                .addAnnotation(Override.class);
        javaEntity.getClassSource().addImport(nodeInterfaceSource);
    }
}
