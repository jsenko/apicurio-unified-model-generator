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
    }


    private IJavaType processPrimitiveType(PrimitiveType typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            return new PrimitiveJavaType(typeModel);
        });
    }


    private IJavaType processEntityType(EntityTypeModel typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            return new EntityJavaType(typeModel);
        });
    }


    private IJavaType processListType(ListTypeModel typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            processAnyType(typeModel.getValueType());
            return new ListJavaType(typeModel, getState().getJavaIndex());
        });
    }


    private IJavaType processMapType(MapTypeModel typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            processAnyType(typeModel.getKeyType());
            processAnyType(typeModel.getValueType());
            return new MapJavaType(typeModel, getState().getJavaIndex());
        });
    }


    private IJavaType processUnionType(UnionTypeModel typeModel) {
        return getState().getJavaIndex().lookupOrIndex(typeModel, () -> {
            typeModel.getTypes().forEach(nested -> {
                processAnyType(nested);
            });
            return new UnionJavaType(typeModel, getState().getConfig().getRootNamespace());
        });
    }


    private IJavaType processAnyType(TypeModel typeModel) {
        if (typeModel.isPrimitiveType()) {
            return processPrimitiveType((PrimitiveType) typeModel);
        } else if (typeModel.isEntityType()) {
            return processEntityType((EntityTypeModel) typeModel);
        } else if (typeModel.isListType()) {
            return processListType((ListTypeModel) typeModel);
        } else if (typeModel.isMapType()) {
            return processMapType((MapTypeModel) typeModel);
        } else if (typeModel.isUnionType()) {
            return processUnionType((UnionTypeModel) typeModel);
        } else {
            fail("Unknown kind of type: %s", typeModel);
            return null;
        }
    }
}
