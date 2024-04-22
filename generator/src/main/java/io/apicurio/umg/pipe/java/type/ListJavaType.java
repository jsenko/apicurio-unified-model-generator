package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.ListTypeModel;
import lombok.Getter;
import org.jboss.forge.roaster.model.source.Importer;

import java.util.List;
import java.util.Map;

public class ListJavaType implements IJavaType {

    @Getter
    private final ListTypeModel typeModel;
    private final JavaIndex index;

    public ListJavaType(ListTypeModel typeModel, JavaIndex index) {
        this.typeModel = typeModel;
        this.index = index;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public String getName() {
        return typeModel.getValueType().getName() + "List";
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(List.class);
        var valueType = index.lookupType(typeModel.getValueType());
        valueType.addImportsTo(importer);
    }

    @Override
    public String toJavaTypeString() {
        var valueType = index.lookupType(typeModel.getValueType());
        return "List<" + valueType.toJavaTypeString() + ">";
    }
}
