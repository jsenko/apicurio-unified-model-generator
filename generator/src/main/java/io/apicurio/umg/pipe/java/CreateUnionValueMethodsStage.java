package io.apicurio.umg.pipe.java;

import java.util.Collection;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.NamespaceModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyModelWithOrigin;
import io.apicurio.umg.pipe.java.method.BodyBuilder;

/**
 * This stage creates method implementations for all union types.  This follows on from
 * the ApplyUnionTypesStage, which adds the union type interfaces to all implementation
 * classes that are needed.  But ApplyUnionTypesStage doesn't create the necessary method
 * implementations to actually implement the interface(s).  This stage is responsible
 * for that.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateUnionValueMethodsStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        getState().getConceptIndex().findEntities("").stream().filter(entity -> entity.isLeaf()).forEach(entity -> {
            Collection<PropertyModelWithOrigin> entityProperties = getState().getConceptIndex().getAllEntityProperties(entity);
            entityProperties.stream().map(property -> property.getProperty()).filter(property -> isUnion(property)).forEach(property -> {
                createUnionValueMethods(property, entity);
            });
        });
    }

    /**
     * @param property
     */
    private void createUnionValueMethods(PropertyModel property, EntityModel origin) {
        UnionPropertyType unionType = new UnionPropertyType(property.getType().getRawType());

        debug("Creating union value methods for property '" + property.getName() + "' of type '"
                + property.getType().getRawType() +"' on entity: " + origin.getNn().fullyQualifiedName());

        unionType.getNestedTypes().forEach(nestedType -> {
            JavaType nestedJT = new JavaType(nestedType, origin.getNn().getNamespace());
            JavaClassSource unionValueImplSource = null;
            String unionValueTypeName = null;

            // For primitives, find the appropriate union value (wrapper) impl class
            // For entities, find the entity implementation class(es)
            // For entity collections (maps/lists), find the appropriate union value (wrapper) class

            if (nestedJT.isPrimitive() || nestedJT.isPrimitiveList() || nestedJT.isPrimitiveMap()) {
                unionValueTypeName = getTypeName(nestedType);
                String unionValueImplFQN = getUnionTypeFQN(unionValueTypeName + "UnionValueImpl");
                unionValueImplSource = getState().getJavaIndex().lookupClass(unionValueImplFQN);
            } else if (nestedJT.isEntity()) {
                unionValueTypeName = nestedType.getSimpleType();
                unionValueImplSource = resolveJavaEntityImpl(origin.getNn().getNamespace().fullName(), unionValueTypeName);
            } else if (nestedJT.isEntityList()) {
                unionValueTypeName = getTypeName(nestedType);
                String unionValueFQN = getUnionTypeFQN(unionValueTypeName + "UnionValueImpl");
                unionValueImplSource = getState().getJavaIndex().lookupClass(unionValueFQN);
            }
            if (unionValueImplSource == null) {
                throw new RuntimeException("[CreateUnionValueMethodsStage] Union type value not supported: " + nestedType);
            }

            createUnionImplMethods(unionType, unionValueImplSource, unionValueTypeName, nestedJT.isEntity(), origin.getNn().getNamespace());
        });
    }

    private void createUnionImplMethods(UnionPropertyType unionType, JavaClassSource unionValueClass,
            String unionValueTypeName, boolean unionValueIsEntity, NamespaceModel nsContext) {

        unionType.getNestedTypes().forEach(nestedType -> {
            String typeName = getTypeName(nestedType);
            String isMethodName = "is" + typeName;
            String asMethodName = "as" + typeName;

            JavaType jt = new JavaType(nestedType, nsContext.fullName()).useCommonEntityResolution();

            String asMethodReturnType = jt.toJavaTypeString();

            if (!unionValueClass.hasMethodSignature(isMethodName)) {
                // Create the "isXyz" method for this type
                MethodSource<JavaClassSource> isMethod = unionValueClass.addMethod().setName(isMethodName).setReturnType(boolean.class).setPublic();
                isMethod.addAnnotation(Override.class);
                BodyBuilder isMethodBody = new BodyBuilder();

                // Create the "asXyz" method for this type
                MethodSource<JavaClassSource> asMethod = unionValueClass.addMethod().setName(asMethodName).setReturnType(asMethodReturnType).setPublic();
                asMethod.addAnnotation(Override.class);
                BodyBuilder asMethodBody = new BodyBuilder();

                if (typeName.equals(unionValueTypeName)) {
                    isMethodBody.append("return true;");
                    isMethod.setBody(isMethodBody.toString());
                    if (nestedType.isEntityType()) {
                        asMethodBody.append("return this;");
                    } else {
                        asMethodBody.append("return getValue();");
                    }
                    asMethod.setBody(asMethodBody.toString());
                } else {
                    isMethodBody.append("return false;");
                    isMethod.setBody(isMethodBody.toString());
                    asMethodBody.append("throw new ClassCastException();");
                    asMethod.setBody(asMethodBody.toString());
                }

                jt.addImportsTo(unionValueClass);
            }
        });

        // If this is an entity, add the "unionValue" method.
        if (unionValueIsEntity && !unionValueClass.hasMethodSignature("unionValue")) {
            MethodSource<JavaClassSource> unionValueMethod = unionValueClass.addMethod().setName("unionValue").setReturnType("Object").setPublic();
            unionValueMethod.addAnnotation(Override.class);
            BodyBuilder unionValueMethodBody = new BodyBuilder();
            unionValueMethodBody.append("return this;");
            unionValueMethod.setBody(unionValueMethodBody.toString());
        }

    }
}
