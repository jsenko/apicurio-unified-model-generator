package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.models.concept.type.EntityTypeModel;
import lombok.Getter;
import org.jboss.forge.roaster.model.source.Importer;

public class EntityJavaType implements IJavaType {

    @Getter
    private final EntityTypeModel typeModel;

    public EntityJavaType(EntityTypeModel typeModel) {
        this.typeModel = typeModel;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public String getName() {
        return typeModel.getName();
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        // TODO?
    }

    @Override
    public String toJavaTypeString() {
        return typeModel.getName();
    }
}
