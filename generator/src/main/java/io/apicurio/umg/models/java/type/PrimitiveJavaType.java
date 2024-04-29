package io.apicurio.umg.models.java.type;

import io.apicurio.umg.models.concept.type.PrimitiveType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import static io.apicurio.umg.logging.Errors.fail;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class PrimitiveJavaType implements IJavaType {

    @Getter
    @EqualsAndHashCode.Include
    private final PrimitiveType typeModel;

    /**
     * When a primitive (this type) is used as a nested type in a union,
     * it has to be wrapped in a "union value type",
     * essentially because we cannot make a primitive extend a union interface.
     * If this is the case, the following contains the generated java interface source,
     * otherwise is null.
     */
    @Getter
    @Setter
    private JavaInterfaceSource unionValueInterfaceSource;

    /**
     * Same as above, but implementation of the interface.
     */
    @Getter
    @Setter
    private JavaClassSource unionValueClassSource;

    public PrimitiveJavaType(PrimitiveType typeModel) {
        this.typeModel = typeModel;
    }

    @Override
    public JavaInterfaceSource getInterfaceSource() {
        return unionValueInterfaceSource;
    }

    @Override
    public void setInterfaceSource(JavaInterfaceSource interfaceSource) {
        this.unionValueInterfaceSource = interfaceSource;
    }

    @Override
    public JavaClassSource getClassSource() {
        return unionValueClassSource;
    }

    @Override
    public void setClassSource(JavaClassSource classSource) {
        this.unionValueClassSource = classSource;
    }

    @Override
    public String getPackageName() {
        fail("TODO");
        return null;
    }

    @Override
    public String getName(boolean _ignored, boolean _ignored2) {
        return typeModel.get_class().getSimpleName();
        //return StringUtils.capitalize(typeModel.getName());
    }

    public JavaInterfaceSource getPrimitiveTypeInterfaceSource() {
        var _class = typeModel.get_class();
        return Roaster.create(JavaInterfaceSource.class)
                .setName(_class.getSimpleName())
                .setPackage(_class.getPackageName());
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(typeModel.get_class());
    }

    @Override
    public String toJavaTypeString(boolean _ignored) {
        return typeModel.get_class().getSimpleName();
    }
}
