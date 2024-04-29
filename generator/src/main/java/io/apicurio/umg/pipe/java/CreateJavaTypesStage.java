package io.apicurio.umg.pipe.java;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.concept.typelike.TraitTypeLike;
import io.apicurio.umg.models.concept.typelike.TypeLikeVisitor;
import io.apicurio.umg.models.java.type.*;
import io.apicurio.umg.pipe.AbstractStage;
import lombok.AllArgsConstructor;

/**
 *
 */
public class CreateJavaTypesStage extends AbstractStage {


    @Override
    protected void doProcess() {
        info("-- Creating Java Types Models --");


        getState().getConceptIndex().getTypes().forEach(typeModel -> {
            var v = new Visitor(getState().getJavaIndex(), getState().getSpecIndex().getNsToPrefix().get(typeModel.getNamespace()));
            typeModel.accept(v);
        });

        // TODO: Add root type
        System.err.println();
    }


    @AllArgsConstructor
    private static class Visitor implements TypeLikeVisitor {

        private JavaIndex index;
        private String prefix;

        @Override
        public void visit(PrimitiveType type) {
            index.lookupOrIndex(type, () -> new PrimitiveJavaType(type));
        }

        @Override
        public void visit(UnionType type) {
            index.lookupOrIndex(type, () -> new UnionJavaType(type, prefix, index));
        }

        @Override
        public void visit(ListType type) {
            index.lookupOrIndex(type, () -> new ListJavaType(type, prefix, index));
        }

        @Override
        public void visit(MapType type) {
            index.lookupOrIndex(type, () -> new MapJavaType(type, prefix, index));
        }

        @Override
        public void visit(EntityType type) {
            index.lookupOrIndex(type, () -> new EntityJavaType(type, prefix));
        }

        @Override
        public void visit(TraitTypeLike type) {
            index.lookupOrIndex(type, () -> new TraitJavaType(type, prefix));
        }
    }
}
