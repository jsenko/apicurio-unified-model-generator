package io.apicurio.umg.pipe.java;

import java.util.List;

import io.apicurio.umg.models.concept.type.MapType;
import io.apicurio.umg.models.concept.type.PrimitiveType;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodHolderSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyModelWithOrigin;
import io.apicurio.umg.models.concept.RawType;

/**
 * Base class for the stages that create methods for entity interfaces and impl classes both.  The
 * logic for these two stages is shared.
 * @author eric.wittmann@gmail.com
 */
public abstract class AbstractCreateMethodsStage extends AbstractJavaStage {

    protected void createPropertyMethods(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {

        PropertyModel property = propertyWithOrigin.getProperty();
        if (property.getName().equals("*")) {
            if (isEntity(property) || isPrimitive(property) || isPrimitiveList(property)) {
                createMappedNodeMethods(javaEntity, propertyWithOrigin);
                if (isEntity(property)) {
                    createFactoryMethod(javaEntity, propertyWithOrigin);
                }
            } else {
                error("STAR property type not handled: " + javaEntity.getCanonicalName() + "::" + property);
                return;
            }
        } else if (property.getName().startsWith("/") && (isEntity(property) || isPrimitive(property))) {
            if (property.getCollection() == null) {
                fail("Regex property defined without a collection name: " + javaEntity.getCanonicalName() + "::" + property);
                return;
            }
            // Given "/foo*/": boolean
            // We need to turn this property into Map<String, Boolean>
            RawType rawCollectionType = RawType.builder()
                    .nested(List.of(property.getType().getRawType()))
                    .map(true)
                    .build();
            var collectionType = MapType.builder()
                    .keyType(PrimitiveType.STRING)
                    .valueType(property.getType())
                    .rawType(rawCollectionType)
                    .build();
            PropertyModel collectionProperty = PropertyModel.builder().name(property.getCollection()).type(collectionType).build();
            PropertyModelWithOrigin collectionPropertyWithOrigin = PropertyModelWithOrigin.builder().property(collectionProperty).origin(propertyWithOrigin.getOrigin()).build();

            if (isEntity(property)) {
                createFactoryMethod(javaEntity, collectionPropertyWithOrigin);
            }
            createGetter(javaEntity, collectionPropertyWithOrigin);
            createAddMethod(javaEntity, collectionPropertyWithOrigin);
            createClearMethod(javaEntity, collectionPropertyWithOrigin);
            createRemoveMethod(javaEntity, collectionPropertyWithOrigin);
        } else if (isPrimitive(property) || isPrimitiveList(property) || isPrimitiveMap(property)) {
            createGetter(javaEntity, propertyWithOrigin);
            createSetter(javaEntity, propertyWithOrigin);
        } else if (isEntity(property)) {
            createGetter(javaEntity, propertyWithOrigin);
            createSetter(javaEntity, propertyWithOrigin);
            createFactoryMethod(javaEntity, propertyWithOrigin);
        } else if (isEntityList(property) || isEntityMap(property)) {
            createFactoryMethod(javaEntity, propertyWithOrigin);
            createGetter(javaEntity, propertyWithOrigin);
            createAddMethod(javaEntity, propertyWithOrigin);
            createClearMethod(javaEntity, propertyWithOrigin);
            createRemoveMethod(javaEntity, propertyWithOrigin);
        } else if (isUnion(property)) {
            createGetter(javaEntity, propertyWithOrigin);
            createSetter(javaEntity, propertyWithOrigin);
            createUnionFactoryMethods(javaEntity, propertyWithOrigin);
        } else {
            warn("Failed to create methods (not yet implemented) for property '" + property.getName() + "' of entity: " + javaEntity.getQualifiedName());
        }
    }

    /**
     * When an entity has a "*" property, that means the entity is a wrapper around a map
     * of values of a particular type.  In this case, the entity needs to extend/implement
     * the "MappedNode" interface.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected abstract void createMappedNodeMethods(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin);

    /**
     * Creates a standard java getter method for the given property.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected void createGetter(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();

        MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setName(getterMethodName(property)).setPublic();
        addAnnotations(method);

        if (isUnion(property)) {
            UnionPropertyType ut = new UnionPropertyType(property.getType().getRawType());
            ut.addImportsTo(javaEntity);
            method.setReturnType(ut.toJavaTypeString());
        } else {
            String propertyOriginNS = propertyWithOrigin.getOrigin().getNn().getNamespace().fullName();

            JavaType jt = new JavaType(property.getType().getRawType(), propertyOriginNS);
            jt.addImportsTo(javaEntity);
            method.setReturnType(jt.toJavaTypeString());
        }

        createGetterBody(property, method);
    }
    abstract protected void createGetterBody(PropertyModel property, MethodSource<?> method);

    /**
     * Creates a standard java setter method for the given property.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected void createSetter(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();
        String propertyOriginNS = propertyWithOrigin.getOrigin().getNn().getNamespace().fullName();

        MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setName(setterMethodName(property)).setReturnTypeVoid().setPublic();
        addAnnotations(method);

        if (isUnion(property)) {
            UnionPropertyType ut = new UnionPropertyType(property.getType().getRawType());
            ut.addImportsTo(javaEntity);
            method.addParameter(ut.toJavaTypeString(), "value");
        } else {
            JavaType jt = new JavaType(property.getType().getRawType(), propertyOriginNS);
            jt.addImportsTo(javaEntity);
            method.addParameter(jt.toJavaTypeString(), "value");
        }

        createSetterBody(property, method);
    }
    abstract protected void createSetterBody(PropertyModel property, MethodSource<?> method);

    /**
     * Creates a factory method for the entity type associated with the given
     * property.  This method will only be called for entity properties, either
     * simple entity properties or collection entity properties (list/map).
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected void createFactoryMethod(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();
        createFactoryMethod(javaEntity, property.getType().getRawType());
    }
    protected void createFactoryMethod(JavaSource<?> javaEntity, RawType propertyType) {
        String _package = javaEntity.getPackage();
        RawType type = propertyType;
        if (type.isMap() || type.isList()) {
            type = type.getNested().iterator().next();
        }
        String entityName = type.getSimpleType();
        String methodName = createMethodName(entityName);
        // The name of the "create" method is based on the type, so it's possible to have
        // duplicates.  Let's not do that.
        if (!hasNamedMethod(((MethodHolderSource<?>) javaEntity), methodName)) {
            JavaInterfaceSource entityType = resolveJavaEntityType(_package, type);
            if (entityType == null) {
                error("Could not resolve entity type: " + _package + "::" + type);
                return;
            }

            MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setPublic().setName(methodName).setReturnType(entityType);
            addAnnotations(method);

            createFactoryMethodBody(javaEntity, entityName, method);
        }
    }
    abstract protected void createFactoryMethodBody(JavaSource<?> javaEntity, String entityName, MethodSource<?> method);

    /**
     * Creates an "add" method for the given property.  The type of the property must be a
     * list of entities.  The add method would accept a single entity and add it to the list.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected void createAddMethod(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();

        String _package = propertyWithOrigin.getOrigin().getNn().getNamespace().fullName();
        RawType type = property.getType().getRawType().getNested().iterator().next();
        String methodName = addMethodName(singularize(property.getName()));
        MethodSource<?> method;

        if (type.isEntityType()) {
            JavaInterfaceSource entityType = resolveJavaEntityType(_package, type);
            if (entityType == null) {
                error("Could not resolve entity type: " + _package + "::" + type);
                return;
            }

            javaEntity.addImport(entityType);

            method = ((MethodHolderSource<?>) javaEntity).addMethod().setPublic().setName(methodName).setReturnTypeVoid();
            addAnnotations(method);
            if (property.getType().isMapType()) {
                method.addParameter("String", "name");
            }
            method.addParameter(entityType.getName(), "value");
        } else if (type.isPrimitiveType()) {
            Class<?> primitiveType = primitiveTypeToClass(type);
            javaEntity.addImport(primitiveType);

            method = ((MethodHolderSource<?>) javaEntity).addMethod().setPublic().setName(methodName).setReturnTypeVoid();
            addAnnotations(method);
            if (property.getType().isMapType()) {
                method.addParameter("String", "name");
            }
            method.addParameter(primitiveType.getSimpleName(), "value");
        } else {
            warn("Type not supported for 'add' method: " + methodName + " with type: " + property.getType());
            return;
        }

        createAddMethodBody(javaEntity, property, method);
    }
    abstract protected void createAddMethodBody(JavaSource<?> javaEntity, PropertyModel property, MethodSource<?> method);

    /**
     * Creates a "clear" method for the given property.  The type of the property must be a
     * list of entities.  The clear method will remove all items from the list.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    protected void createClearMethod(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();

        String methodName = clearMethodName(property.getName());

        MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setPublic().setName(methodName).setReturnTypeVoid();
        addAnnotations(method);

        createClearMethodBody(property, method);
    }
    abstract protected void createClearMethodBody(PropertyModel property, MethodSource<?> method);

    /**
     * Creates a "remove" method for the given property.  The type of the property must be a
     * list of entities.  The remove method will remove one item from the list.
     */
    protected void createRemoveMethod(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();

        String _package = propertyWithOrigin.getOrigin().getNn().getNamespace().fullName();
        String methodName = removeMethodName(singularize(property.getName()));
        MethodSource<?> method = ((MethodHolderSource<?>) javaEntity).addMethod().setPublic().setName(methodName).setReturnTypeVoid();
        addAnnotations(method);

        if (property.getType().isListType()) {
            RawType type = property.getType().getRawType().getNested().iterator().next();
            JavaInterfaceSource entityType = resolveJavaEntityType(_package, type);
            if (entityType == null) {
                error("Could not resolve entity type: " + _package + "::" + type);
                return;
            }
            javaEntity.addImport(entityType);
            method.addParameter(entityType.getName(), "value");
        } else {
            method.addParameter("String", "name");
        }

        createRemoveMethodBody(property, method);
    }
    abstract protected void createRemoveMethodBody(PropertyModel property, MethodSource<?> method);

    /**
     * Create factory methods for any entity types in the union.  If the union is, for example, "boolean|string"
     * then this will do nothing.  But if the union is "Widget|string" then a factory method for Widgets will
     * be created.
     * @param javaEntity
     * @param propertyWithOrigin
     */
    private void createUnionFactoryMethods(JavaSource<?> javaEntity, PropertyModelWithOrigin propertyWithOrigin) {
        PropertyModel property = propertyWithOrigin.getProperty();
        UnionPropertyType ut = new UnionPropertyType(property.getType().getRawType());
        ut.getNestedTypes().forEach(nestedType -> {
            if (nestedType.isEntityType()) {
                createFactoryMethod(javaEntity, nestedType);
            } else if ((nestedType.isList() || nestedType.isMap()) && nestedType.getNested().iterator().next().isEntityType()) {
                createFactoryMethod(javaEntity, nestedType.getNested().iterator().next());
            }
        });
    }

    /**
     * Gives subclasses an opportunity to add annotations to the created method.
     * @param method
     */
    protected void addAnnotations(MethodSource<?> method) {
    }

}
