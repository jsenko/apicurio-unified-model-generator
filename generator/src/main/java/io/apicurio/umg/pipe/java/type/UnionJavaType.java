package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.models.concept.RawType;
import io.apicurio.umg.models.concept.type.UnionTypeModel;
import lombok.Getter;
import lombok.Setter;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.List;

import static io.apicurio.umg.pipe.java.AbstractJavaStage.getTypeName;

public class UnionJavaType implements IJavaType {

    private String rootNamespace;

    private String name;

    @Getter
    private UnionTypeModel typeModel;

    @Getter
    @Setter
    private JavaInterfaceSource interfaceSource;

    public UnionJavaType(UnionTypeModel typeModel, String rootNamespace) {
        this.rootNamespace = rootNamespace;
        // Check if the model has a different name than the raw type representation
        // Supports aliases
        if (typeModel.getName().equals(typeModel.getRawType().asRawType())) {
            this.name = getUnionTypeName(typeModel.getRawType().getNested());
        } else {
            this.name = typeModel.getName();
        }
        this.typeModel = typeModel;
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(getFQN());
    }

    public String getName() {
        return name;
    }

    @Override
    public String toJavaTypeString() {
        return name;
    }

    public String getPackageName() {
        return rootNamespace + ".union";
    }

    public String getFQN() {
        return getPackageName() + "." + name;
    }

    public String getUnionInterfaceFQN() {
        return getPackageName() + ".Union";
    }

    private static String getUnionTypeName(List<RawType> unionNestedTypes) {
        return unionNestedTypes.stream().map(pt -> getTypeName(pt)).reduce((t, u) -> t + u).orElseThrow() + "Union";
    }
}
