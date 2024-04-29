package io.apicurio.umg.models.java.type;

import io.apicurio.umg.models.concept.type.EntityType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class EntityJavaType implements IJavaType {

    private JavaInterfaceSource interfaceSource;

    private JavaClassSource classSource;

    @Getter
    @EqualsAndHashCode.Include
    private final EntityType typeModel;
    private final String prefix;

    public EntityJavaType(EntityType typeModel, String prefix) {
        this.typeModel = typeModel;
        this.prefix = prefix;
    }

//    @Override
//    public String getPackageName() {
//        return null;
//    }

    @Override
    public String getName(boolean withPrefix, boolean impl) {
        return (withPrefix ? prefix : "") + typeModel.getName() + (impl ? "Impl" : "");
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        if(interfaceSource != null) {
            importer.addImport(interfaceSource);
        }
        if(classSource != null) {
            importer.addImport(classSource);
        }
    }

    @Override
    public String toJavaTypeString(boolean impl) {
        return getName(true, impl);
    }
}
