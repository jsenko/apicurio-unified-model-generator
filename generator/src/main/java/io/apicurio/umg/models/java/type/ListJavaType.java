package io.apicurio.umg.models.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.ListType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.List;

@ToString
public class ListJavaType extends CollectionJavaType {


    public ListJavaType(ListType typeModel, String prefix, JavaIndex index) {
        super(typeModel, prefix, index);
    }

    @Override
    public String getName(boolean withPrefix, boolean impl) {
        // TODO This has to be the java type name instead
        var valueType = index.lookupType(typeModel.getValueType());
        return (withPrefix ? prefix : "") + valueType.getName(false, impl) + "List";
    }

    @Override
    public void addImportsTo(Importer<?> importer) {
        importer.addImport(List.class);
        var valueType = index.lookupType(typeModel.getValueType());
        valueType.addImportsTo(importer);
    }

    @Override
    public String toJavaTypeString(boolean impl) {
        var valueType = index.lookupType(typeModel.getValueType());
        return "List<" + valueType.toJavaTypeString(impl) + ">";
    }

    @Override
    public String toJavaTypeStringWithExtends() {
        var valueType = index.lookupType(typeModel.getValueType());
        return "List<? extends " + valueType.toJavaTypeString(false) + ">";
    }
}
