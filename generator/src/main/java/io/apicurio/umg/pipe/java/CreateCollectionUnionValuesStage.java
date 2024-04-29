package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.concept.type.UnionType;
import io.apicurio.umg.models.concept.typelike.TypeLike;
import io.apicurio.umg.models.java.type.*;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import static io.apicurio.umg.logging.Errors.assertion;

/**
 * Ensures that any missing union value/wrapper classes are created.  Some of the wrapper classes
 * are currently manually curated (see {@link LoadBaseClassesStage}) but entity list and map
 * wrappers must be generated.  This stage does that.  It will potentially result in the generation
 * of new wrapper interfaces and classes (e.g. WidgetListUnionValue and WidgetListUnionValueImpl).
 * These interfaces and classes will be generated in the <em>{rootPackage}.union</em> package.
 */
public class CreateCollectionUnionValuesStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        // Find list types that are inside a union,
        // then map to the equivalent Java type.
        getState().getConceptIndex().getTypes().stream()
                .filter(TypeLike::isUnionType)
                .flatMap(t -> ((UnionType) t).getTypes().stream())
                .filter(Type::isCollectionType)
                .map(t -> (CollectionJavaType) getState().getJavaIndex().getTypeIndex().get(t))
                .forEach(this::createListUnionValues);
        System.err.println();
    }

    private void createListUnionValues(CollectionJavaType javaType) {

        if (javaType.getUnionValueInterfaceSource() != null) {
            return; // We've already processed this value (see primitive type case)
        }

        var isList = javaType.getTypeModel().isListType();

        // Get the interface source for the union value type
        JavaInterfaceSource valueJavaInterfaceSource = null;
        IJavaType valueJavaType = getState().getJavaIndex().lookupType(javaType.getTypeModel().getValueType());

        if (valueJavaType.getTypeModel().isEntityType()) {
            valueJavaInterfaceSource = ((EntityJavaType) valueJavaType).getInterfaceSource();
        }
        if (valueJavaType.getTypeModel().isUnionType()) {
            valueJavaInterfaceSource = ((UnionJavaType) valueJavaType).getInterfaceSource();
        }
        if (valueJavaType.getTypeModel().isPrimitiveType()) {
            // We only create value type for the oldest parent
            if (javaType.getTypeModel().getParent() != null) {
                // process parent first
                var parentJavaType = (CollectionJavaType) getState().getJavaIndex().requireType(javaType.getTypeModel().getParent());
                createListUnionValues(parentJavaType);
                // and set the value type to the one from the parent
                javaType.setUnionValueInterfaceSource(parentJavaType.getUnionValueInterfaceSource());
                javaType.setUnionValueClassSource(parentJavaType.getUnionValueClassSource());
                return;
            } else {
                // continue as normal
                valueJavaInterfaceSource = ((PrimitiveJavaType) valueJavaType).getPrimitiveTypeInterfaceSource();
            }
        }
        assertion(valueJavaInterfaceSource != null, "Nested collection types are not supported yet.");

        String unionValueName = javaType.getName(true, false) + "UnionValue";
        String unionValueImplName = unionValueName + "Impl";
        String unionValuePackage = javaType.getPackageName();

        var entityCollectionUnionValueName = isList ? "ListUnionValue" : "MapUnionValue";
        String entityCollectionUnionValueImplName = entityCollectionUnionValueName + "Impl";
        String entityCollectionUnionValueFQN = getUnionTypeFQN(entityCollectionUnionValueName);
        String entityCollectionUnionValueImplFQN = getUnionTypeFQN(entityCollectionUnionValueImplName);

        JavaInterfaceSource entityCollectionUnionValueSource = getState().getJavaIndex().lookupInterface(entityCollectionUnionValueFQN);
        JavaClassSource entityCollectionUnionValueImplSource = getState().getJavaIndex().lookupClass(entityCollectionUnionValueImplFQN);

        // Create the union value wrapper interface
        JavaInterfaceSource unionTypeInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueName)
                .setPublic();
        unionTypeInterface.addImport(entityCollectionUnionValueSource);
        unionTypeInterface.addImport(valueJavaInterfaceSource);
        unionTypeInterface.addInterface(entityCollectionUnionValueName + "<" + valueJavaInterfaceSource.getName() + ">");

        javaType.setUnionValueInterfaceSource(unionTypeInterface);
        getState().getJavaIndex().index(unionTypeInterface);

        // Now create the union value wrapper class (impl)
        JavaClassSource unionTypeImplClass = Roaster.create(JavaClassSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueImplName)
                .setPublic();
        unionTypeImplClass.addImport(entityCollectionUnionValueImplSource);
        unionTypeImplClass.addImport(valueJavaInterfaceSource);
        // Implements the correct interface (generated above).
        unionTypeImplClass.addInterface(unionTypeInterface);
        // Extends the right base class.
        unionTypeImplClass.setSuperType(entityCollectionUnionValueImplName + "<" + valueJavaInterfaceSource.getName() + ">");

        // Add default constructor
        MethodSource<JavaClassSource> defaultConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        defaultConstructor.setBody("super();");

        // Add list/map constructor
        MethodSource<JavaClassSource> valueConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        if (isList) {
            valueConstructor.addParameter("List<" + valueJavaInterfaceSource.getName() + ">", "value");
        } else {
            valueConstructor.addParameter("Map<String, " + valueJavaInterfaceSource.getName() + ">", "value");
        }
        valueConstructor.setBody("super(value);");

        /* Override
         * boolean isUnionValueListWithNodes() or
         * boolean isUnionValueMapWithNodes()
         * if appropriate.
         */
        if (javaType.getTypeModel().getValueType().isEntityType() || javaType.getTypeModel().getValueType().isUnionType()) {
            if (isList) {
                unionTypeImplClass.addMethod()
                        .setName("isListUnionValueWithAny")
                        .setReturnType(boolean.class)
                        .setBody("return true;")
                        .setPublic()
                        .addAnnotation(Override.class);
            } else {
                unionTypeImplClass.addMethod()
                        .setName("isMapUnionValueWithAny")
                        .setReturnType(boolean.class)
                        .setBody("return true;")
                        .setPublic()
                        .addAnnotation(Override.class);
            }
        }

        javaType.setUnionValueClassSource(unionTypeImplClass);
        getState().getJavaIndex().index(unionTypeImplClass);
    }
}
