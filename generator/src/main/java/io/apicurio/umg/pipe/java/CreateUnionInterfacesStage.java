package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.java.type.UnionJavaType;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * Creates the union type interface.  For example, if the union type is "boolean|[string]" then
 * a new Java interface named "BooleanStringListUnion" will be created to represent a property
 * that can be either a boolean or a string list.
 * <p>
 * Also for example, if there is a union type property that is "number|Widget", then a new Java
 * interface named "NumberWidgetUnion" will be created to represent that property (the value of
 * which can be either a number or a Widget entity).
 */
public class CreateUnionInterfacesStage extends AbstractJavaStage {


    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isUnionType())
                .forEach(t -> createUnionType((UnionJavaType) t));
        System.err.println();
    }


    private void createUnionType(UnionJavaType union) {

        // Recurse and handle the parent first, if we have not already, because we might extend it
        UnionJavaType parent = null;
        if (union.getTypeModel().getParent() != null) {
            parent = ((UnionJavaType) getState().getJavaIndex().requireType(union.getTypeModel().getParent()));
            if (parent.getInterfaceSource() == null) {
                createUnionType(parent);
            }
        }

        String name = union.getName(true, false);
        String _package = union.getPackageName();//getUnionTypesPackageName();

        // Create the main union type interface
        JavaInterfaceSource unionTypeInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(_package)
                .setName(name)
                .setPublic();


        if (parent == null) {
            // It must extend the "Union" interface, but only if it has no parent type
            String unionFQN = getUnionInterfaceFQN();
            JavaInterfaceSource unionValueSource = getState().getJavaIndex().lookupInterface(unionFQN);
            unionTypeInterface.addImport(unionValueSource);
            unionTypeInterface.addInterface(unionValueSource);
        } else {
            // otherwise it must extend the parent type
            unionTypeInterface.addImport(parent.getInterfaceSource());
            unionTypeInterface.addInterface(parent.getInterfaceSource());
        }

        union.setInterfaceSource(unionTypeInterface);
        getState().getJavaIndex().index(unionTypeInterface);
    }
}
