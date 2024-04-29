package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.java.type.CollectionJavaType;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.PrimitiveJavaType;
import io.apicurio.umg.models.java.type.UnionJavaType;
import io.apicurio.umg.pipe.java.method.UnionAsMethod;
import io.apicurio.umg.pipe.java.method.UnionIsMethod;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.HashSet;
import java.util.Set;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.Utils.addIfNotNull;

/**
 * This stage creates method implementations for all union types.  This follows on from
 * the ApplyUnionTypesStage, which adds the union type interfaces to all implementation
 * classes that are needed.  But ApplyUnionTypesStage doesn't create the necessary method
 * implementations to actually implement the interface(s).  This stage is responsible
 * for that.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateUnionClassMethodsStage extends AbstractJavaStage {


    @Override
    protected void doProcess() {
        getState().getJavaIndex().getTypeIndex().values().stream()
                .filter(t -> t.getTypeModel().isUnionType())
                .forEach(t -> createUnionClassMethods((UnionJavaType) t));
        System.err.println();
    }


    private void createUnionClassMethods(UnionJavaType union) {

        union.getTypeModel().getTypes().forEach(nestedType -> { // TODO Nested iterators? We're only reading the collection

            var javaType = getState().getJavaIndex().lookupType(nestedType);
            assertion(javaType != null);

            // We have the collection of sources for *this* nested type, but we have to mark them inactive for all *other* nestedTypes
            // Example:
            // union = Schema|[Schema], active is Schema, which is effectively {ArraySchema, ObjectSchema},
            // but inactive is [Schema] which is SchemaList

            // ACTIVE:
            Set<JavaClassSource> activeUnionValueSources = new HashSet<>();
            collectUnionValueSources(nestedType, activeUnionValueSources);
            activeUnionValueSources.forEach(s -> {
                // TODO: Remove hack
                if (!s.getSuperType().contains("PrimitiveUnionValueImpl") || union.getTypeModel().getParent() == null) {
                    // Prevent duplicate methods
                    if(!s.hasMethodSignature(UnionIsMethod.methodName(javaType))) {
                        UnionIsMethod.create(getState(), union, s, true, javaType, true);
                        UnionAsMethod.create(getState(), union, s, true, javaType, true);
                    }
                }
            });

            // INACTIVE:
            Set<JavaClassSource> inactiveUnionValueSources = new HashSet<>();
            union.getTypeModel().getTypes().stream().filter(t -> !t.equals(nestedType)).forEach(t -> {
                collectUnionValueSources(t, inactiveUnionValueSources);
            });
            inactiveUnionValueSources.forEach(s -> {
                // TODO: Remove hack
                if (!s.getSuperType().contains("PrimitiveUnionValueImpl") || union.getTypeModel().getParent() == null) {
                    // Prevent duplicate methods
                    if(!s.hasMethodSignature(UnionIsMethod.methodName(javaType))) {
                        UnionIsMethod.create(getState(), union, s, true, javaType, false);
                        UnionAsMethod.create(getState(), union, s, true, javaType, false);
                    }
                }
            });


            // TODO
            // every union has a "unionValue" method, so the synthetic union values can return the "pure" union type.
            // If this is an entity, add the "unionValue" method, which simply returns itself.
            if (javaType instanceof EntityJavaType) {
                var source = ((EntityJavaType) javaType).getClassSource();
                if (source != null && !source.hasMethodSignature("getUnionValue")) {
                    source.addMethod()
                            .setName("getUnionValue")
                            .setReturnType(javaType.toJavaTypeString(false))
                            .setBody("return this;")
                            .setPublic()
                            .addAnnotation(Override.class);
                }
            }
        });
    }


    void collectUnionValueSources(Type type, Set<JavaClassSource> result) {
        var javaType = getState().getJavaIndex().lookupType(type);
        assertion(javaType != null);

        if (javaType instanceof EntityJavaType) {
            addIfNotNull(result, (((EntityJavaType) javaType).getClassSource()));
        } else if (javaType instanceof CollectionJavaType) {
            addIfNotNull(result, (((CollectionJavaType) javaType).getUnionValueClassSource()));
        } else if (javaType instanceof UnionJavaType) {
            // We need to recurse, for example:
            // union = Schema|SchemaList
            // and schema is nested to ArraySchema|ObjectSchema,
            // so both ArraySchema and ObjectSchema need to have method isSchema, asSchema (active), and isSchemaList, asSchemaList (inactive)
            ((UnionJavaType) javaType).getTypeModel().getTypes().forEach(t -> {
                collectUnionValueSources(t, result);
            });
        } else if (javaType instanceof PrimitiveJavaType) {
            addIfNotNull(result, (((PrimitiveJavaType) javaType).getUnionValueClassSource()));
        } else {
            fail("TODO");
        }
    }
}
