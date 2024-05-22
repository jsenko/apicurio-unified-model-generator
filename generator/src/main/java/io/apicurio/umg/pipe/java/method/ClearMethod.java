package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.JavaField;

import static io.apicurio.umg.pipe.java.method.JavaUtils.markOverridden;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class ClearMethod {

    public static void create(JavaField field, boolean body) {

        var method = field.getMethodSource().addMethod()
                .setName(methodName(field.getProperty()))
                .setReturnTypeVoid();

        if (body) {
            markOverridden(method);
            method.setBody(
                    BodyBuilder.create()
                            .c("fieldName", field.getFieldName())
                            .a("if (this.${fieldName} != null) {")
                            .a("    this.${fieldName}.clear();")
                            .a("}")
                            .toString()
            );
            method.setPublic();
        }
    }


    private static String methodName(PropertyModel property) {
        return "clear" + capitalize(property.getName());
    }
}
