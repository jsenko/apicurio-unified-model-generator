package io.apicurio.umg.pipe.java;

import io.apicurio.umg.pipe.java.type.UnionJavaType;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * Creates the union type interface.  For example, if the union type is "boolean|[string]" then
 * a new Java interface named "BooleanStringListUnion" will be created to represent a property
 * that can be either a boolean or a string list.
 *
 * Also for example, if there is a union type property that is "number|Widget", then a new Java
 * interface named "NumberWidgetUnion" will be created to represent that property (the value of
 * which can be either a number or a Widget entity).
 */
public class CreateUnionTypesStage extends AbstractJavaStage {


    @Override
    protected void doProcess() {
        //getState().getConceptIndex().getTypes().stream()
        //        .filter(t -> t.isUnionType())
        //        .forEach(t -> createUnionType((UnionTypeModel) t));
        getState().getJavaIndex().getTypeIndex().entrySet().stream()
                .filter(e -> e.getKey().isUnionType())
                .forEach(e -> createUnionInterfaceSource((UnionJavaType) e.getValue()));
    }


    private void createUnionInterfaceSource(UnionJavaType union) {
        debug("Creating union type for: %s", union);
        //UnionPropertyType unionType = new UnionPropertyType(property.getProperty().getType().getRawType());
        //var unionJavaType = new UnionJavaType(union, getState().getConfig().getRootNamespace());

        String name = union.getName();
        String _package = union.getPackageName();

        // Create the main union type interface
        JavaInterfaceSource unionTypeInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(_package)
                .setName(name)
                .setPublic();

        union.setInterfaceSource(unionTypeInterface);
        getState().getJavaIndex().index(unionTypeInterface);

        // It must extend the "Union" interface
        String unionFQN = union.getUnionInterfaceFQN();
        JavaInterfaceSource unionValueSource = getState().getJavaIndex().lookupInterface(unionFQN);
        unionTypeInterface.addImport(unionValueSource);
        unionTypeInterface.addInterface(unionValueSource);

        // Now create the union methods.
        createUnionMethods(union);
    }

    private void createUnionMethods(UnionJavaType unionJavaType) {
        unionJavaType.getTypeModel().getTypes().forEach(nestedType -> {

            var javaType = getState().getJavaIndex().lookupType(nestedType);

            String typeName = javaType.getName();//getTypeName(nestedType);
            String isMethodName = "is" + typeName;
            String asMethodName = "as" + typeName;

            //JavaType jt = new JavaType(nestedType, nsContext.fullName()).useCommonEntityResolution();

            String asMethodReturnType = javaType.toJavaTypeString();//jt.toJavaTypeString();

            unionJavaType.getInterfaceSource().addMethod().setName(isMethodName).setReturnType(boolean.class).setPublic();
            unionJavaType.getInterfaceSource().addMethod().setName(asMethodName).setReturnType(asMethodReturnType).setPublic();
            //jt.addImportsTo(unionTypeInterface);
            javaType.addImportsTo(unionJavaType.getInterfaceSource()); // TODO

        });
    }
}
