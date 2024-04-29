package io.apicurio.umg.pipe.java;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.MapType;
import io.apicurio.umg.models.java.JavaField;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.models.java.type.MapJavaType;
import io.apicurio.umg.pipe.java.method.JavaUtils;

import java.util.Collection;
import java.util.HashSet;

import static io.apicurio.umg.models.concept.ConceptUtils.asStringMapOf;

/**
 * Creates the fields for each entity implementation.  This is done by iterating over all leaf entities
 * and collecting all the properties for it.  One field is created for each property.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateFieldsStage extends AbstractJavaStage {

    @Override
    protected void doProcess() {

        getState().getJavaIndex().getTypeIndex().values()
                .stream()
                .filter(t -> t.getTypeModel().isEntityType())
                .map(t -> (EntityJavaType) t)
                .filter(et -> et.getTypeModel().isLeaf())
                .forEach(et -> {
                    mergeProperties(et).forEach(p -> {

                        //if (et.getClassSource() != null) {
                        var field = new JavaField(
                                et.getClassSource(),
                                p, getState().getJavaIndex().requireType(p.getType())
                        );
                        createField(field);
                        //}
                    });
                });
        System.err.println();
    }

    private static Collection<PropertyModel> mergeProperties(EntityJavaType javaType) {
        var res = new HashSet<PropertyModel>();
        res.addAll(javaType.getTypeModel().getEntity().getProperties().values());
        javaType.getTypeModel().getEntity().getTraits().forEach(t -> res.addAll(t.getProperties().values()));
        return res;
    }

    private void createField(JavaField field) {
        // TODO star properties and mapped nodes
        if (field.getProperty().isStar()) {
            return; // We're skipping this, TODO move to filter
        }

        var fieldName = JavaUtils.sanitizeFieldName(field.getProperty().getEffectiveName());

        IJavaType fieldType = null;

        if (field.getProperty().isRegex()) {
            var collectionProperty = asStringMapOf(field.getProperty().getCollection(), field.getProperty());
            fieldType = new MapJavaType((MapType) collectionProperty.getType(), getPrefix(field.getProperty().getType().getNamespace()), getState().getJavaIndex());
            //field.setProperty(collectionProperty);
        } else {
            fieldType = field.getType();
        }

        var ancestorType = fieldType;
        while (ancestorType.getTypeModel().getParent() != null) {
            ancestorType = getState().getJavaIndex().requireType(ancestorType.getTypeModel().getParent());
        }

        field.getFieldSource().addField()
                .setPrivate()
                .setType(ancestorType.toJavaTypeString(false))
                .setName(fieldName);
    }
}
