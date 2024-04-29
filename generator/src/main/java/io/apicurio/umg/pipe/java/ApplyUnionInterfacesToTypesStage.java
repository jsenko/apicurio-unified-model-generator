package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.java.type.CollectionJavaType;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.PrimitiveJavaType;
import io.apicurio.umg.models.java.type.UnionJavaType;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;

/**
 * A union type has already been created (as an interface like StringWidgetUnion) and now must be
 * applied.  This means that the impl classes for "String" and "Widget" (from the StringWidgetUnion
 * example) must implement the union type interface.  For primitive types and collection types, we need
 * to update the wrapper/value interfaces to extend the union type interface.  For entity types
 * we update the generated entity interfaces.
 * <p>
 * To continue with the example above, we need to update the "StringUnionValue" and "Widget" interfaces
 * to extend the "StringWidgetUnion" union type.
 */
public class ApplyUnionInterfacesToTypesStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isUnionType())
                .forEach(t -> applyUnionType((UnionJavaType) t));
        System.err.println();
    }


    private void applyUnionType(UnionJavaType union) {

        union.getTypeModel().getTypes().forEach(nestedType -> {
            var javaType = getState().getJavaIndex().lookupType(nestedType);
            assertion(javaType != null);

            JavaInterfaceSource unionTypeSource = union.getInterfaceSource();

            // TODO Visitor pattern?
            if (javaType instanceof EntityJavaType) {
                ((EntityJavaType) javaType).getInterfaceSource().addImport(unionTypeSource);
                ((EntityJavaType) javaType).getInterfaceSource().addInterface(unionTypeSource);
            } else if (javaType instanceof CollectionJavaType) {
                ((CollectionJavaType) javaType).getUnionValueInterfaceSource().addImport(unionTypeSource);
                ((CollectionJavaType) javaType).getUnionValueInterfaceSource().addInterface(unionTypeSource);
            } else if (javaType instanceof UnionJavaType) {
                ((UnionJavaType) javaType).getInterfaceSource().addImport(unionTypeSource);
                ((UnionJavaType) javaType).getInterfaceSource().addInterface(unionTypeSource);
                // Remove the Union interface from the nested type, since it'll be inherited
                ((UnionJavaType) javaType).getInterfaceSource().removeInterface(getState().getJavaIndex().lookupInterface(getUnionInterfaceFQN()));
            } else if (javaType instanceof PrimitiveJavaType) {
                // Only add implements if the union type is the oldest parent,
                // because the children extend it anyway and for primitive types it does not matter
                if (union.getTypeModel().getParent() == null) {
                    ((PrimitiveJavaType) javaType).getUnionValueInterfaceSource().addImport(unionTypeSource);
                    ((PrimitiveJavaType) javaType).getUnionValueInterfaceSource().addInterface(unionTypeSource);
                }
            } else {
                fail("TODO");
            }
        });
        System.err.println();
    }
}
