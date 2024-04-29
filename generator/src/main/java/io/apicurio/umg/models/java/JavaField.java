package io.apicurio.umg.models.java;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.type.IJavaType;
import lombok.*;
import org.jboss.forge.roaster.model.source.FieldHolderSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;

import static io.apicurio.umg.pipe.java.method.JavaUtils.sanitizeFieldName;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class JavaField {

    /**
     * Either or both of the following fields MUST not be null.
     * If they are both non-null, they MUST refer to the same class source.
     */
    private MethodHolderSource<?> methodSource;
    private FieldHolderSource<?> fieldSource;

    private PropertyModel property;
    private IJavaType type;


    public JavaField(JavaInterfaceSource interfaceSource, PropertyModel property, IJavaType type) {
        this(interfaceSource, null, property, type);
    }

    public JavaField(JavaClassSource classSource, PropertyModel property, IJavaType type) {
        this(classSource, classSource, property, type);
    }

    public String getFieldName() {
        var name = property.getName();
        if ("*".equals(name)) {
            return "_items";
        }
        if (name.startsWith("/")) {
            name = property.getCollection();
        }
        return sanitizeFieldName(name);
    }
}
