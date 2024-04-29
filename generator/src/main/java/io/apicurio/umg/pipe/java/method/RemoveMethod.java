package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.models.java.type.ListJavaType;
import io.apicurio.umg.models.java.type.MapJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.jboss.forge.roaster.model.source.Importer;

import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.java.method.JavaUtils.singularize;
import static org.apache.commons.lang3.StringUtils.capitalize;

public class RemoveMethod {

    public static void create(GeneratorState state, JavaField field, boolean body) {

        var method = field.getMethodSource().addMethod()
                .setName(methodName(field.getProperty()))
                .setReturnTypeVoid();

        if (field.getType() instanceof ListJavaType) {

            var nested = ((ListJavaType) field.getType()).getTypeModel().getValueType();
            var nestedJavaType = state.getJavaIndex().requireType(nested);

            var ancestorType = nestedJavaType;
            while (ancestorType.getTypeModel().getParent() != null) {
                ancestorType = state.getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
            }

            method.addParameter(ancestorType.toJavaTypeString(false), "value");
            field.getType().addImportsTo((Importer<?>) field.getMethodSource());

            if (body) {
                method.addAnnotation(Override.class);
                method.setBody(
                        BodyBuilder.create()
                                .c("fieldName", field.getFieldName())
                                .a("if (this.${fieldName} != null) {")
                                .a("    this.${fieldName}.remove(value);")
                                .a("}")
                                .toString()
                );
                method.setPublic();
            }

        } else if (field.getType() instanceof MapJavaType) {

            method.addParameter("String", "name");
            ((Importer<?>) field.getMethodSource()).addImport(String.class);

            if (body) {
                method.addAnnotation(Override.class);
                method.setBody(
                        BodyBuilder.create()
                                .c("fieldName", field.getFieldName())
                                .a("if (this.${fieldName} != null) {")
                                .a("    this.${fieldName}.remove(name);")
                                .a("}")
                                .toString()
                );
                method.setPublic();
            }

        } else {
            fail("TODO");
        }
    }


    private static String methodName(PropertyModel property) {
        return "remove" + capitalize(singularize(property.getName()));
    }
}
