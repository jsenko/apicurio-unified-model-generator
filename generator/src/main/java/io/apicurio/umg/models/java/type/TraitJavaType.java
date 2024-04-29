package io.apicurio.umg.models.java.type;

import io.apicurio.umg.models.concept.typelike.TraitTypeLike;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class TraitJavaType implements IJavaType {

    private JavaInterfaceSource interfaceSource;

    @Getter
    @EqualsAndHashCode.Include
    private final TraitTypeLike typeModel;

    private final String prefix;

    public TraitJavaType(TraitTypeLike typeModel, String prefix) {
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
        if (interfaceSource != null) {
            importer.addImport(interfaceSource);
        }
    }

    @Override
    public String toJavaTypeString(boolean impl) {
        return getName(true, impl);
    }
}
