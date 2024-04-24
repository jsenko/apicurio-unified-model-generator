package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyModelWithOrigin;
import io.apicurio.umg.models.concept.RawType;
import io.apicurio.umg.pipe.java.AbstractJavaStage;
import io.apicurio.umg.pipe.java.Util;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class GetterMethod {
/*
    protected void createGetter(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();

        MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setName(getterMethodName(property)).setPublic();
        addAnnotations(method);

        if (isUnion(property)) {
            AbstractJavaStage.UnionPropertyType ut = new AbstractJavaStage.UnionPropertyType(property.getType().getRawType());
            ut.addImportsTo(javaEntity);
            method.setReturnType(ut.toJavaTypeString());
        } else {
            String propertyOriginNS = propertyWithOrigin.getOrigin().getNn().getNamespace().fullName();

            AbstractJavaStage.JavaType jt = new AbstractJavaStage.JavaType(property.getType().getRawType(), propertyOriginNS);
            jt.addImportsTo(javaEntity);
            method.setReturnType(jt.toJavaTypeString());
        }

        createGetterBody(property, method);
    }

    protected void createGetterBody(PropertyModel property, MethodSource<?> method) {
        String fieldName = getFieldName(property);
        BodyBuilder body = new BodyBuilder();
        body.addContext("fieldName", fieldName);
        body.append("return ${fieldName};");
        method.setBody(body.toString());
    }

    protected String getFieldName(PropertyModel property) {
        if (property.getName().equals("*")) {
            return "_items";
        }
        if (property.getName().startsWith("/")) {
            return sanitizeFieldName(property.getCollection());
        }
        return sanitizeFieldName(property.getName());
    }

    protected String sanitizeFieldName(String name) {
        if (name == null) {
            return null;
        }
        return Util.JAVA_KEYWORD_MAP.getOrDefault(name, name);
    }

    protected String getterMethodName(PropertyModel propertyModel) {
        String name = propertyModel.getName();
        if (name.startsWith("/")) {
            name = propertyModel.getCollection();
        }
        return getterMethodName(name, propertyModel.getType().getRawType());
    }

    protected String getterMethodName(String propertyName, RawType type) {
        boolean isBool = type.isPrimitiveType() && type.getSimpleType().equals("boolean");
        return (isBool ? "is" : "get") + StringUtils.capitalize(propertyName);
    }

 */
}
