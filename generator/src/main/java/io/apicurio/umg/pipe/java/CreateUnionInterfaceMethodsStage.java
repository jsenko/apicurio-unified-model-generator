package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.java.type.UnionJavaType;
import io.apicurio.umg.pipe.java.method.UnionAsMethod;
import io.apicurio.umg.pipe.java.method.UnionIsMethod;

import static io.apicurio.umg.pipe.java.method.JavaUtils.collectNestedJavaTypes;

/**
 * Creates the union type interface.  For example, if the union type is "boolean|[string]" then
 * a new Java interface named "BooleanStringListUnion" will be created to represent a property
 * that can be either a boolean or a string list.
 * <p>
 * Also for example, if there is a union type property that is "number|Widget", then a new Java
 * interface named "NumberWidgetUnion" will be created to represent that property (the value of
 * which can be either a number or a Widget entity).
 */
public class CreateUnionInterfaceMethodsStage extends AbstractJavaStage {


    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isUnionType())
                .forEach(t -> createUnionMethods((UnionJavaType) t));
        System.err.println();
    }


    private void createUnionMethods(UnionJavaType union) {

        union.getTypeModel().getTypes().forEach(nestedType -> {
            var javaType = getState().getJavaIndex().requireType(nestedType);

            if (union.getTypeModel().getParent() == null) {
                UnionIsMethod.create(getState(), union, union.getInterfaceSource(), false, javaType, false);
            }
            // If the type does not have any nested entity types, we can avoid overriding.
            if (union.getTypeModel().getParent() == null || collectNestedJavaTypes(getState(), javaType).stream().anyMatch(t -> t.getTypeModel().isEntityType())) {
                UnionAsMethod.create(getState(), union, union.getInterfaceSource(), false, javaType, false);
            }
            javaType.addImportsTo(union.getInterfaceSource()); // TODO
        });
    }
}
