package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.Importer;

import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.java.method.JavaUtils.*;

public class CreateFactoryMethod {


    public static void createForNested(GeneratorState state, JavaField field, boolean body) {
        collectNestedJavaTypes(state, field.getType())
                .stream()
                .filter(t -> t.getTypeModel().isEntityType())
                .forEach(t -> {
                    create(state, new JavaField(field.getMethodSource(), field.getFieldSource(), field.getProperty(), t), body);
                });
    }

    public static void create(GeneratorState state, JavaField field, boolean body) {

        if (!field.getType().getTypeModel().isEntityType()) {
            fail("TODO");
        }

        var type = (EntityJavaType) field.getType();

        // TODO
        var ancestorType = field.getType();
        while (ancestorType.getTypeModel().getParent() != null) {
            ancestorType = state.getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
        }

        if (hasMethod(field.getMethodSource(), methodName(type))) {
            return;
        }

        var method = field.getMethodSource().addMethod()
                .setName(methodName(type))
                .setReturnType(ancestorType.toJavaTypeString(false));

        ancestorType.addImportsTo((Importer<?>) field.getMethodSource());

        if (body) {
            markOverridden(method);
            method.setBody(
                    BodyBuilder.create()
                            .c("class", ancestorType.toJavaTypeString(false))
                            .c("implClass", type.toJavaTypeString(true))
                            .a("${class} node = new ${implClass}();")
                            .a("node.attachTo(this);")
                            .a("return node;")
                            .toString()
            );
            method.setPublic();
        }
    }


    private static String methodName(IJavaType type) {
        return "create" + StringUtils.capitalize(type.getName(false, false));
    }
}
