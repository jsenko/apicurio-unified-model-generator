package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.MapType;
import lombok.Getter;
import org.jboss.forge.roaster.model.source.Importer;

import java.util.Map;

public class MapJavaType implements IJavaType {

    @Getter
    private final MapType typeModel;
    private final JavaIndex index; // This replaces nesting, maybe we want to nest?

    public MapJavaType(MapType typeModel, JavaIndex index) {
        this.typeModel = typeModel;
        this.index = index;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public String getName() {
        return typeModel.getValueType().getName() + "Map";
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(Map.class);
        importer.addImport(String.class);
        var valueType = index.lookupType(typeModel.getValueType());
        valueType.addImportsTo(importer);
    }

    @Override
    public String toJavaTypeString() {
        var valueType = index.lookupType(typeModel.getValueType());
        return "Map<String, " + valueType.toJavaTypeString() + ">";
    }
}
