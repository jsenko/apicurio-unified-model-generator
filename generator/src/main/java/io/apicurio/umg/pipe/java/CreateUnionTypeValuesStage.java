package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.NamespaceModel;
import io.apicurio.umg.models.concept.RawType;
import io.apicurio.umg.models.concept.type.CollectionTypeModel;
import io.apicurio.umg.models.concept.type.EntityTypeModel;
import io.apicurio.umg.models.concept.type.UnionTypeModel;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;

/**
 * Ensures that any missing union value/wrapper classes are created.  Some of the wrapper classes
 * are currently manually curated (see {@link LoadBaseClassesStage}) but entity list and map
 * wrappers must be generated.  This stage does that.  It will potentially result in the generation
 * of new wrapper interfaces and classes (e.g. WidgetListUnionValue and WidgetListUnionValueImpl).
 * These interfaces and classes will be generated in the <em>{rootPackage}.union</em> package.
 * <p>
 * If one of the nested type of a union is a collection, it cannot implement a common interface directly.
 * So we have to create a wrapper. For example:
 * [Widget] -> WidgetListUnionValue extends EntityUnionValue (manual)
 */
public class CreateUnionTypeValuesStage extends AbstractJavaStage {


    @Override
    protected void doProcess() {
        getState().getConceptIndex().getTypes().stream()
                .filter(t -> t.isUnionType())
                .forEach(t -> createMissingUnionValues((UnionTypeModel) t));
    }


    private void createMissingUnionValues(UnionTypeModel union) {
        union.getTypes().forEach(nestedType -> {
            if (nestedType.isCollectionType()) {
                var collectionType = (CollectionTypeModel) nestedType;
                // If the nested type is a list or map
                var itemType = collectionType.getValueType();
                if (itemType.isEntityType()) {
                    String typeName = getTypeName(itemType.getRawType());
                    String unionValueName = typeName + "UnionValue";
                    String unionValueFQN = getUnionTypeFQN(unionValueName);

                    // Make sure to only create the union value/wrapper once.
                    JavaInterfaceSource entityCollectionUnionValueSource = getState().getJavaIndex().lookupInterface(unionValueFQN);
                    if (entityCollectionUnionValueSource == null) {
                        createEntityCollectionUnionValue(((EntityTypeModel) itemType).getEntity().getNn().getNamespace(), itemType.getRawType(), itemType.isListType());
                    }
                } else {
                    fail("This kind of union is not supported: %s", union.getRawType());
                }
            }
        });
    }


    /**
     * Creates a union value type wrapper interface and impl class for an entity list or map.  This allows
     * a union type to be formed that includes an entity list/map type.
     *
     * @param namespace
     * @param entityType
     * @param isList
     */
    private void createEntityCollectionUnionValue(NamespaceModel namespace, RawType entityType, boolean isList) {
        String typeName = getTypeName(entityType);
        String mapOrList = isList ? "List" : "Map";
        String unionValueName = typeName + mapOrList + "UnionValue";
        String unionValueImplName = unionValueName + "Impl";
        String unionValuePackage = getUnionTypesPackageName();

        // Base class/interface
        String entityCollectionUnionValueName = "Entity" + mapOrList + "UnionValue";
        String entityCollectionUnionValueImplName = entityCollectionUnionValueName + "Impl";
        String entityCollectionUnionValueFQN = getUnionTypeFQN(entityCollectionUnionValueName);
        String entityCollectionUnionValueImplFQN = getUnionTypeFQN(entityCollectionUnionValueImplName);

        JavaInterfaceSource entitySource = resolveCommonJavaEntity(namespace, entityType.getSimpleType());
        JavaInterfaceSource entityCollectionUnionValueSource = getState().getJavaIndex().lookupInterface(entityCollectionUnionValueFQN);
        JavaClassSource entityCollectionUnionValueImplSource = getState().getJavaIndex().lookupClass(entityCollectionUnionValueImplFQN);

        // Create the union value wrapper interface
        JavaInterfaceSource unionTypeInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueName)
                .setPublic();
        unionTypeInterface.addImport(entityCollectionUnionValueSource);
        unionTypeInterface.addImport(entitySource);
        if (isList) {
            unionTypeInterface.addInterface(entityCollectionUnionValueName + "<" + entitySource.getName() + ">");
        } else {
            unionTypeInterface.addInterface(entityCollectionUnionValueName + "<String, " + entitySource.getName() + ">");
        }
        getState().getJavaIndex().index(unionTypeInterface);

        // Now create the union value wrapper class (impl)
        JavaClassSource unionTypeImplClass = Roaster.create(JavaClassSource.class)
                .setPackage(unionValuePackage)
                .setName(unionValueImplName)
                .setPublic();
        unionTypeImplClass.addImport(entityCollectionUnionValueImplSource);
        unionTypeImplClass.addImport(entitySource);
        // Implements the correct interface (generated above).
        unionTypeImplClass.addInterface(unionTypeInterface);
        // Extends the right base class.
        if (isList) {
            unionTypeImplClass.setSuperType(entityCollectionUnionValueImplName + "<" + entitySource.getName() + ">");
        } else {
            unionTypeImplClass.setSuperType(entityCollectionUnionValueImplName + "<String, " + entitySource.getName() + ">");
        }

        // Add default constructor
        MethodSource<JavaClassSource> defaultConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        defaultConstructor.setBody("super();");

        // Add list/map constructor
        MethodSource<JavaClassSource> valueConstructor = unionTypeImplClass.addMethod().setPublic().setConstructor(true);
        valueConstructor.addParameter("List<" + entitySource.getName() + ">", "value");
        valueConstructor.setBody("super(value);");

        getState().getJavaIndex().index(unionTypeImplClass);
    }

}
