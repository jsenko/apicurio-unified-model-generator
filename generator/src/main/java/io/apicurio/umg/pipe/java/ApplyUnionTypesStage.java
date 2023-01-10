package io.apicurio.umg.pipe.java;

import io.apicurio.umg.pipe.java.type.UnionJavaType;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * A union type has already been created (as an interface like StringWidgetUnion) and now must be
 * applied.  This means that the impl classes for "String" and "Widget" (from the StringWidgetUnion
 * example) must implement the union type interface.  For primitive types and collection types, we need
 * to update the wrapper/value interfaces to extend the union type interface.  For entity types
 * we update the generated entity interfaces.
 *
 * To continue with the example above, we need to update the "StringUnionValue" and "Widget" interfaces
 * to extend the "StringWidgetUnion" union type.
 */
public class ApplyUnionTypesStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().entrySet().stream()
                .filter(e -> e.getKey().isUnionType())
                .forEach(e -> applyUnionType((UnionJavaType) e.getValue()));
    }


    private void applyUnionType(UnionJavaType union) {

        //UnionPropertyType unionType = new UnionPropertyType(property.getProperty().getType());
        //String unionTypeFQN = getUnionTypeFQN(unionType.getName());
        JavaInterfaceSource unionTypeSource = union.getInterfaceSource();//getState().getJavaIndex().lookupInterface(unionTypeFQN);

        union.getTypeModel().getTypes().forEach(nestedType -> {
            var nestedJT = getState().getJavaIndex().lookupType(nestedType);
            JavaInterfaceSource unionValueSource = null;

            if (nestedType.isPrimitiveType() || nestedType.isPrimitiveListType() || nestedType.isPrimitiveMapType() || nestedType.isEntityListType()) {
                String typeName = nestedJT.getName();
                String unionValueFQN = getUnionTypeFQN(typeName + "UnionValue");
                unionValueSource = getState().getJavaIndex().lookupInterface(unionValueFQN);
            }
            else if(nestedType.isEntityType()) {
                // TODO nestedJT should contain a reference to value source
                unionValueSource = resolveJavaEntity(nestedType.getContextNamespace(), nestedType.getName());
            } else if(nestedType.isUnionType()) {
                String typeName = nestedJT.getName();
                String unionValueFQN = getUnionTypeFQN(typeName);
                unionValueSource = getState().getJavaIndex().lookupInterface(unionValueFQN);
            }
            if(unionValueSource == null) {
                fail("[ApplyUnionTypesStage] Union type value not supported: %s", nestedType);
            }

            unionValueSource.addImport(unionTypeSource);
            unionValueSource.addInterface(unionTypeSource);
            System.err.println();
        });
    }
}
