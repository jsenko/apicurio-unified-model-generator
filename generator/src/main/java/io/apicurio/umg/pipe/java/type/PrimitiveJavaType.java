package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.models.concept.type.PrimitiveType;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.Importer;

public class PrimitiveJavaType implements IJavaType {

    @Getter
    private final PrimitiveType typeModel;

    public PrimitiveJavaType(PrimitiveType typeModel) {
        this.typeModel = typeModel;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public String getName() {
        return StringUtils.capitalize(typeModel.getName()); // or typeModel.get_class().getSimpleName() ?
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(typeModel.get_class());
    }

    @Override
    public String toJavaTypeString() {
        return typeModel.get_class().getSimpleName();
    }
}
