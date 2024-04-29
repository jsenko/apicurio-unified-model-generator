package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.type.MapType;
import io.apicurio.umg.models.concept.typelike.TraitTypeLike;
import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.models.java.type.MapJavaType;
import io.apicurio.umg.pipe.java.method.*;

import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.models.concept.ConceptUtils.asStringMapOf;
import static io.apicurio.umg.pipe.java.method.JavaUtils.extractProperties;

/**
 * Adds methods to all entity interfaces. This works by finding all the properties for the entity and then
 * deciding what methods should exist on the entity interface based on the name and type of the property.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateMethodsStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {
        // TODO Traits!

        getState().getJavaIndex().getTypeIndex().values()
                .stream()
                .filter(t -> t.getTypeModel().isEntityType() || t.getTypeModel().isTraitTypeLike())
                .filter(t -> !t.getTypeModel().isTraitTypeLike() || !((TraitTypeLike) t.getTypeModel()).getTrait().isTransparent())
                .forEach(et -> {
                    // Interfaces
                    extractProperties(et, false).forEach(p -> {
                        if (!p.isShadowed()) {
                            var field = new JavaField(
                                    et.getInterfaceSource(),
                                    p, getState().getJavaIndex().requireType(p.getType())
                            );
                            createMethods(field, false);
                        }
                    });
                    var props = extractProperties(et, true);
                    props.forEach(p -> {
                        if (et.getTypeModel().isEntityType()) {
                            var field = new JavaField(
                                    et.getInterfaceSource(),
                                    p, getState().getJavaIndex().requireType(p.getType())
                            );
                            createCreateMethod(field, false);
                        }
                    });
                    // Classes
                    props.forEach(p -> {
                        if (et.getTypeModel().isEntityType() && et.getTypeModel().isLeaf() && et.getClassSource() != null) {
                            var field = new JavaField(
                                    et.getClassSource(),
                                    p, getState().getJavaIndex().requireType(p.getType())
                            );
                            createCreateMethod(field, true);
                            createMethods(field, true);
                        }
                    });
                });
        System.err.println();
    }


    private void createCreateMethod(JavaField field, boolean body) {
        // Factory - createX methods for all nested entities
        // We need to special-case this, because unlike for entity interfaces, where we don't want to include
        // getter/setter methods from parents, the create methods are based on all properties.
        CreateFactoryMethod.createForNested(getState(), field, body);
    }

    private void createMethods(JavaField field, boolean body) {

        if (field.getProperty().getName().startsWith("/")) {
            if (field.getProperty().getCollection() == null) {
                fail("Regex property defined without a collection name: %s", field);
                return;
            }

            var collectionProperty = asStringMapOf(field.getProperty().getCollection(), field.getProperty());
            var collectionJavaType = new MapJavaType((MapType) collectionProperty.getType(), getPrefix(field.getProperty().getType().getNamespace()), getState().getJavaIndex());

            field.setProperty(collectionProperty);
            field.setType(collectionJavaType);
        }

        // Getter
        if (!"*".equals(field.getProperty().getName())) {
            GetterMethod.create(getState(), field, body);
        }

        // Setter
        if (!"*".equals(field.getProperty().getName()) && !field.getProperty().getName().startsWith("/")) {
            SetterMethod.create(getState(), field, body);
        }

        // Collection properties
        if (!"*".equals(field.getProperty().getName()) && field.getType().getTypeModel().isCollectionType()) {
            AddMethod.create(getState(), field, body);
            RemoveMethod.create(getState(), field, body);
            ClearMethod.create(field, body);
        }
    }
}
