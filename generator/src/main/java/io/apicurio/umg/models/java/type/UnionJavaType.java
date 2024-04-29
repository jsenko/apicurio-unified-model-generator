package io.apicurio.umg.models.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.UnionType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UnionJavaType implements IJavaType {

    //private String rootNamespace;
    private final String prefix;

    private final JavaIndex index;

    private String name;

    @Getter
    @EqualsAndHashCode.Include
    private UnionType typeModel;

    @Getter
    @Setter
    private JavaInterfaceSource interfaceSource;

    public UnionJavaType(UnionType typeModel, String prefix, JavaIndex index) {
        //this.rootNamespace = rootNamespace;
        this.prefix = prefix;
        this.index = index;
        // Check if the model has a different name than the raw type representation
        // Supports aliases
        if (typeModel.getName().equals(typeModel.getRawType().asRawType())) {
            this.name = typeModel.getTypes().stream().map(t -> {
                var valueType = index.lookupType(t);
                return valueType.getName(false, false);
            }).reduce((t, u) -> t + u).orElseThrow() + "Union";
        } else {
            this.name = typeModel.getName();
        }
        this.typeModel = typeModel;
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        if (interfaceSource != null) {
            importer.addImport(interfaceSource);
        }
        typeModel.getTypes().forEach(t -> {
            var valueType = index.lookupType(t);
            valueType.addImportsTo(importer);
        });
    }

    @Override
    public String getName(boolean withPrefix, boolean impl) {
        return (withPrefix ? prefix : "") + name + (impl ? "Impl" : "");
    }

    @Override
    public String toJavaTypeString(boolean impl) {
        return getName(true, impl);
    }

//    public String getPackageName() {
//        return typeModel.getNamespace();
//        //return rootNamespace + ".union";
//    }

    public String getFQN() {
        return getPackageName() + "." + name;
    }

}
