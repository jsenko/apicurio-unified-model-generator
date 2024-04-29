package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.concept.type.UnionType;
import io.apicurio.umg.models.concept.typelike.TypeLike;
import io.apicurio.umg.models.java.type.PrimitiveJavaType;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Ensures that any missing union value/wrapper classes are created.  Some of the wrapper classes
 * are currently manually curated (see {@link LoadBaseClassesStage}) but entity list and map
 * wrappers must be generated.  This stage does that.  It will potentially result in the generation
 * of new wrapper interfaces and classes (e.g. WidgetListUnionValue and WidgetListUnionValueImpl).
 * These interfaces and classes will be generated in the <em>{rootPackage}.union</em> package.
 */
public class CreatePrimitiveUnionValuesStage extends AbstractJavaStage {

    /**
     * We need to process each primitive type only once.
     */
    private Set<PrimitiveJavaType> processed = new HashSet<>();

    @Override
    protected void doProcess() {
        // Find primitive types that are inside a union,
        // then map to the equivalent Java type.
        getState().getConceptIndex().getTypes().stream()
                .filter(TypeLike::isUnionType)
                .flatMap(t -> ((UnionType) t).getTypes().stream())
                .filter(Type::isPrimitiveType)
                .map(t -> (PrimitiveJavaType) getState().getJavaIndex().getTypeIndex().get(t))
                .forEach(this::createPrimitiveUnionValues);
        System.err.println();
    }

    private void createPrimitiveUnionValues(PrimitiveJavaType javaType) {

        if(processed.contains(javaType)) {
            return;
        }
        processed.add(javaType);

        var valueJavaInterfaceSource = javaType.getPrimitiveTypeInterfaceSource();

        String unionValueName = javaType.getName(true, false) + "UnionValue";
        String unionValueImplName = unionValueName + "Impl";
        String unionValuePackage = getUnionTypesPackageName();

        var baseUnionValueName = "PrimitiveUnionValue";
        var baseUnionValueImplName = baseUnionValueName + "Impl";
        var baseUnionValueFQN = getUnionTypeFQN(baseUnionValueName);
        var baseUnionValueImplFQN = getUnionTypeFQN(baseUnionValueImplName);

        JavaInterfaceSource baseUnionValueSource = getState().getJavaIndex().lookupInterface(baseUnionValueFQN);
        JavaClassSource baseUnionValueImplSource = getState().getJavaIndex().lookupClass(baseUnionValueImplFQN);

        // Create the union value wrapper interface
        JavaInterfaceSource unionTypeInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueName)
                .setPublic();
        unionTypeInterface.addImport(baseUnionValueSource);
        unionTypeInterface.addImport(valueJavaInterfaceSource);
        unionTypeInterface.addInterface(baseUnionValueName + "<" + valueJavaInterfaceSource.getName() + ">");

        javaType.setUnionValueInterfaceSource(unionTypeInterface);
        getState().getJavaIndex().index(unionTypeInterface);

        // Now create the union value wrapper class (impl)
        JavaClassSource unionTypeImplClass = Roaster.create(JavaClassSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueImplName)
                .setPublic();
        unionTypeImplClass.addImport(baseUnionValueImplSource);
        unionTypeImplClass.addImport(valueJavaInterfaceSource);
        // Implements the correct interface (generated above).
        unionTypeImplClass.addInterface(unionTypeInterface);
        // Extends the right base class.
        unionTypeImplClass.setSuperType(baseUnionValueImplName + "<" + valueJavaInterfaceSource.getName() + ">");

        // Add default constructor
        MethodSource<JavaClassSource> defaultConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        defaultConstructor.setBody("super();");

        // Add list/map constructor
        MethodSource<JavaClassSource> valueConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        valueConstructor.addParameter(valueJavaInterfaceSource.getName(), "value");
        valueConstructor.setBody("super(value);");

        javaType.setUnionValueClassSource(unionTypeImplClass);
        getState().getJavaIndex().index(unionTypeImplClass);
    }
}
