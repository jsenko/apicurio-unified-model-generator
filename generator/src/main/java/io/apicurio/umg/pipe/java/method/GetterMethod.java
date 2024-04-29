package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.PrimitiveType;
import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.Importer;

public class GetterMethod {

    public static void create(GeneratorState state, JavaField field, boolean body) {

        var ancestorType = field.getType();
        while (ancestorType.getTypeModel().getParent() != null) {
            ancestorType = state.getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
        }

        var method = field.getMethodSource().addMethod()
                .setName(methodName(field.getProperty(), field.getType()))
                .setReturnType(ancestorType.toJavaTypeString(false));

        field.getType().addImportsTo((Importer<?>) field.getMethodSource());

        if (body) {
            method.addAnnotation(Override.class);
            method.setBody(
                    BodyBuilder.create()
                            .c("fieldName", field.getFieldName())
                            .a("return ${fieldName};")
                            .toString()
            );
            method.setPublic();
        }
    }


    public static String methodName(PropertyModel property, IJavaType returnType) {
        String name = property.getName();
        if (name.startsWith("/")) {
            name = property.getCollection();
        }
        boolean isBool = returnType.getTypeModel().isPrimitiveType() && ((PrimitiveType) returnType.getTypeModel()).get_class().equals(Boolean.class);
        return (isBool ? "is" : "get") + StringUtils.capitalize(name);
    }
}
