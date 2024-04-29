package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.type.EntityJavaType;

import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;

/**
 * Creates the java interfaces for all entities.
 *
 * @author eric.wittmann@gmail.com
 */
public class ApplyNodeOrMappedNodeStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {

        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isEntityType())
                .map(t -> (EntityJavaType) t)
                .forEach(this::applyNodeOrMappedNode);
        System.err.println();
    }

    private void applyNodeOrMappedNode(EntityJavaType type) {

        var starProperty = type.getTypeModel().getEntity().getProperties().values().stream().filter(PropertyModel::isStar).collect(Collectors.toList());
        assertion(starProperty.size() <= 1);

        if (starProperty.size() == 0) {
            // If we have a parent, we inherit the interface
            if (type.getInterfaceSource() != null && !type.getTypeModel().getEntity().hasParent()) {
                var node = getState().getJavaIndex().lookupInterface(getState().getConfig().getRootNamespace() + ".Node");
                type.getInterfaceSource().addImport(node);
                type.getInterfaceSource().addInterface(node);
            }
            if (type.getClassSource() != null) {
                var nodeImpl = getState().getJavaIndex().lookupClass(getState().getConfig().getRootNamespace() + ".NodeImpl");
                type.getClassSource().addImport(nodeImpl);
                type.getClassSource().extendSuperType(nodeImpl);
            }
        } else {
            var starPropertyJavaType = getState().getJavaIndex().requireType(starProperty.get(0).getType());
            // TODO Util method!
            var ancestorType = starPropertyJavaType;
            while (ancestorType.getTypeModel().getParent() != null) {
                ancestorType = getState().getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
            }

            // If we have a parent with a start property, we inherit the interface
            if (type.getInterfaceSource() != null && (!type.getTypeModel().getEntity().hasParent() || !type.getTypeModel().getEntity().getParent().hasProperty("*"))) {
                var node = getState().getJavaIndex().lookupInterface(getState().getConfig().getRootNamespace() + ".MappedNode");
                type.getInterfaceSource().addImport(node);
                type.getInterfaceSource().addInterface(node.getName() + "<" + ancestorType.toJavaTypeString(false) + ">");
                ancestorType.addImportsTo(type.getInterfaceSource());
            }
            if (type.getClassSource() != null) {
                var nodeImpl = getState().getJavaIndex().lookupClass(getState().getConfig().getRootNamespace() + ".MappedNodeImpl");
                type.getClassSource().addImport(nodeImpl);
                type.getClassSource().setSuperType(nodeImpl.getName() + "<" + ancestorType.toJavaTypeString(false) + ">");
                ancestorType.addImportsTo(type.getClassSource());
            }
        }
    }
}