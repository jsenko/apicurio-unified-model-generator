package io.apicurio.umg.models.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.MapType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.Importer;

import java.util.Map;

@ToString
public class MapJavaType extends CollectionJavaType {


    public MapJavaType(MapType typeModel, String prefix, JavaIndex index) {
        super(typeModel, prefix, index);
    }

    @Override
    public String getName(boolean withPrefix, boolean impl) {
        var valueType = index.lookupType(typeModel.getValueType());
        return (withPrefix ? prefix : "") + valueType.getName(false, impl) + "Map";
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(Map.class);
        importer.addImport(String.class);
        var valueType = index.lookupType(typeModel.getValueType());
        valueType.addImportsTo(importer);
    }

    @Override
    public String toJavaTypeString(boolean impl) {
        var valueType = index.lookupType(typeModel.getValueType());
        return "Map<String, " + valueType.toJavaTypeString(impl) + ">";
    }

    @Override
    public String toJavaTypeStringWithExtends() {
        var valueType = index.lookupType(typeModel.getValueType());
        return "Map<String, ? extends " + valueType.toJavaTypeString(false) + ">";
    }
}
