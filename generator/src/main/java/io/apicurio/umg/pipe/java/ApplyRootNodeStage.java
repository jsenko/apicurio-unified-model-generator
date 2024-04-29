package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.models.java.type.UnionJavaType;
import io.apicurio.umg.pipe.java.method.BodyBuilder;
import io.apicurio.umg.pipe.java.method.JavaUtils;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;

/**
 * Creates the java interfaces for all entities.
 *
 * @author eric.wittmann@gmail.com
 */
public class ApplyRootNodeStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {

        var types = getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isType() && ((Type) t.getTypeModel()).isRoot())
                .collect(Collectors.toList());

        assertion(types.size() > 0, "Root type must be defined, and must not be a trait.");

        var problems = types.stream()
                .flatMap(t -> JavaUtils.collectNestedJavaTypes(getState(), t).stream())
                .filter(t -> !t.getTypeModel().isEntityType() && !t.getTypeModel().isUnionType())
                .map(t -> ((Type) t.getTypeModel()).getRawType().asRawType())
                .collect(Collectors.toList());

        if (problems.size() > 0) {
            fail("All (nested) root types must be entities, these are not: %s", String.join(" ", problems));
        }

        types.forEach(t -> applyRootNode(t, false));

        types.stream()
                .flatMap(t -> JavaUtils.collectNestedJavaTypes(getState(), t).stream())
                .filter(t -> !types.contains(t))
                .collect(Collectors.toSet()).forEach(t -> applyRootNode(t, true));

        System.err.println();
    }

    private void applyRootNode(IJavaType type, boolean skipRootCapable) {

        var root = getState().getJavaIndex().lookupInterface(getState().getConfig().getRootNamespace() + ".RootCapable");
        var modelType = getState().getJavaIndex().lookupEnum(getState().getConfig().getRootNamespace() + ".ModelType");

        JavaInterfaceSource interfaceSource = null;
        JavaClassSource classSource = null;

        if (type instanceof UnionJavaType) {
            interfaceSource = ((UnionJavaType) type).getInterfaceSource();
        }
        if (type instanceof EntityJavaType) {
            interfaceSource = ((EntityJavaType) type).getInterfaceSource();
            classSource = ((EntityJavaType) type).getClassSource();
        }


        // Only the parent type needs to implement RootCapable
        if (!skipRootCapable && interfaceSource != null && type.getTypeModel().getParent() == null) {
            interfaceSource.addImport(root);
            interfaceSource.addInterface(root);
        }

        // Otherwise
        if (classSource != null) {

            if (type.getTypeModel().isLeaf()) {
                interfaceSource.addMethod()
                        .setName("createRoot")
                        .setStatic(true)
                        .setReturnType(type.toJavaTypeString(false))
                        .setBody(BodyBuilder.create()
                                .c("type", type.toJavaTypeString(true))
                                .c("modelType", prefixToModelType(((EntityJavaType) type).getPrefix()))
                                .a("return new ${type}(ModelType.${modelType});")
                                .toString()
                        );

                interfaceSource.addImport(modelType);
            }

            classSource.addImport(root);
            classSource.addImport(modelType);

            classSource.addField()
                    .setType("ModelType")
                    .setName("_modelType")
                    .setPrivate();

            classSource.addMethod()
                    .setConstructor(true)
                    .setBody(BodyBuilder.create()
                            .a("this._modelType = modelType;")
                            .toString()
                    )
                    .setPackagePrivate()
                    .addParameter("ModelType", "modelType");

            classSource.addMethod()
                    .setConstructor(true)
                    .setBody(BodyBuilder.create()
                            .a("super();")
                            .toString()
                    )
                    .setPublic();

            classSource.addMethod()
                    .setName("isRoot")
                    .setReturnType(boolean.class)
                    .setBody(BodyBuilder.create()
                            .a("return this._modelType != null;")
                            .toString()
                    )
                    .setPublic()
                    .addAnnotation(Override.class);

            classSource.addMethod()
                    .setName("modelType")
                    .setReturnType("ModelType")
                    .setBody(BodyBuilder.create()
                            .a("if(this.isRoot()) {")
                            .a("    return this._modelType;")
                            .a("} else {")
                            .a("    throw new IllegalStateException(\"This node is not a root.\");")
                            .a("}")
                            .toString()
                    )
                    .setPublic()
                    .addAnnotation(Override.class);

            classSource.addMethod()
                    .setName("root")
                    .setReturnType("RootCapable")
                    .setBody(BodyBuilder.create()
                            .a("if(this.isRoot()) {")
                            .a("    return this;")
                            .a("} else {")
                            .a("    return super.root();")
                            .a("}")
                            .toString()
                    )
                    .setPublic()
                    .addAnnotation(Override.class);

            classSource.addMethod()
                    .setName("parent")
                    .setReturnType("Node")
                    .setBody(BodyBuilder.create()
                            .a("if(this.isRoot()) {")
                            .a("    return this;")
                            .a("} else {")
                            .a("    return super.parent();")
                            .a("}")
                            .toString()
                    )
                    .setPublic()
                    .addAnnotation(Override.class);

            var m = classSource.addMethod()
                    .setName("attachTo");
            m.addParameter("Node", "parent");
            m.setBody(BodyBuilder.create()
                            .a("super.attachTo(parent);")
                            .a("if(this.isRoot()) {")
                            .a("    this._modelType = null;")
                            .a("}")
                            .toString()
                    )
                    .setPublic()
                    .addAnnotation(Override.class);
        }
    }
}