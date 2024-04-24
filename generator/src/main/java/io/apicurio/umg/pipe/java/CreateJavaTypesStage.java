package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.pipe.AbstractStage;
import io.apicurio.umg.pipe.java.type.*;

/**
 *
 */
public class CreateJavaTypesStage extends AbstractStage {


    @Override
    protected void doProcess() {
        info("-- Creating Java Types Models --");

        getState().getConceptIndex().getTypes().forEach(typeModel -> {
            processAnyType(typeModel);
        });
        // TODO Add root type
        System.err.println();
    }


    private IJavaType processPrimitiveType(PrimitiveType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            return new PrimitiveJavaType(typeModel);
        });
    }


    private IJavaType processEntityType(EntityType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            return new EntityJavaType(typeModel);
        });
    }


    private IJavaType processListType(ListType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            processAnyType(typeModel.getValueType());
            return new ListJavaType(typeModel, getState().getJavaIndex());
        });
    }


    private IJavaType processMapType(MapType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            processAnyType(typeModel.getKeyType());
            processAnyType(typeModel.getValueType());
            return new MapJavaType(typeModel, getState().getJavaIndex());
        });
    }


    private IJavaType processUnionType(UnionType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            typeModel.getTypes().forEach(nested -> {
                processAnyType(nested);
            });
            return new UnionJavaType(typeModel, getState().getConfig().getRootNamespace());
        });
    }


    private IJavaType processAnyType(Type type) {
        if (type.isPrimitiveType()) {
            return processPrimitiveType((PrimitiveType) type);
        } else if (type.isEntityType()) {
            return processEntityType((EntityType) type);
        } else if (type.isListType()) {
            return processListType((ListType) type);
        } else if (type.isMapType()) {
            return processMapType((MapType) type);
        } else if (type.isUnionType()) {
            return processUnionType((UnionType) type);
        } else {
            fail("Unknown kind of type: %s", type);
            return null;
        }
    }
}
