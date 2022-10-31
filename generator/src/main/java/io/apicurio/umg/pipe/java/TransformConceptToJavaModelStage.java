package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.NamespaceModel;
import io.apicurio.umg.models.java.JavaClassModel;
import io.apicurio.umg.models.java.JavaFieldModel;
import io.apicurio.umg.models.java.JavaInterfaceModel;
import io.apicurio.umg.models.java.JavaPackageModel;
import io.apicurio.umg.pipe.AbstractStage;

public class TransformConceptToJavaModelStage extends AbstractStage {
    @Override
    protected void doProcess() {

        getState().getConceptIndex().getAllEntitiesWithCopy().forEach(entity -> {
            makePackages(entity.getNamespace());
            var _package = getState().getJavaIndex().getPackages().get(entity.getNamespace().fullName());
            var _class = (JavaClassModel) getState().getJavaIndex().lookupAndIndexType(() -> {
                return JavaClassModel.builder()
                        ._package(_package)
                        .name(entity.getName())
                        .build();
            });

            entity.getProperties().values().forEach(p -> {
                _class.getFields().add(JavaFieldModel.builder()
                        .name(p.getName())
                        .concept(p)
                        .build());
            });

            _class.setEntityModel(entity);
        });

        getState().getConceptIndex().getAllTraitsWithCopy().forEach(trait -> {
            makePackages(trait.getNamespace());
            var _package = getState().getJavaIndex().getPackages().get(trait.getNamespace().fullName());
            var _interface = getState().getJavaIndex().lookupAndIndexType(() -> {
                return JavaInterfaceModel.builder()
                        ._package(_package)
                        .name(trait.getName())
                        .build();
            });
            trait.getProperties().values().forEach(p -> {
                _interface.getFields().add(JavaFieldModel.builder()
                        .name(p.getName())
                        .concept(p)
                        .build());
            });

            _interface.setTraitModel(trait);
        });
    }


    private void makePackages(NamespaceModel namespace) {
        JavaPackageModel child = null;
        while (namespace != null) {
            NamespaceModel finalNamespace = namespace;
            var _package = getState().getJavaIndex().lookupAndIndexPackage(() -> {
                return JavaPackageModel.builder()
                        .name(finalNamespace.fullName())
                        .build();
            });
            if (child != null) {
                child.setParent(_package);
                _package.getChildren().put(child.getName(), child);
            }
            child = _package;
            namespace = namespace.getParent();
        }
    }
}
