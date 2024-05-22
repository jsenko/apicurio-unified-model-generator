package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.pipe.GeneratorState;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.Importer;

import static io.apicurio.umg.pipe.java.method.JavaUtils.markOverridden;

public class SetterMethod {

    public static void create(GeneratorState state, JavaField field, boolean body) {

        // For a getter, we need to find the oldest ancestor, due to contravariant parameter
        // TODO Make this a util method
        var ancestorType = field.getType();
        while (ancestorType.getTypeModel().getParent() != null) {
            ancestorType = state.getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
        }

        var method = field.getMethodSource().addMethod()
                .setName(methodName(field.getProperty()))
                .setReturnTypeVoid();

        method.addParameter(ancestorType.toJavaTypeString(false), "value");

        ancestorType.addImportsTo((Importer<?>) field.getMethodSource());

        if (body) {

            var bb = BodyBuilder.create();
            bb.c("fieldName", field.getFieldName())
                    .a("this.${fieldName} = value;");

            markOverridden(method);
            method.setBody(bb.toString());
            method.setPublic();
        }
    }


    public static String methodName(PropertyModel property) {
        return "set" + StringUtils.capitalize(property.getName());
    }
}
