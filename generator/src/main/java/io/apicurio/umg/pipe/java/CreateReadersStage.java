package io.apicurio.umg.pipe.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyType;
import io.apicurio.umg.models.concept.TraitModel;
import io.apicurio.umg.models.java.JavaClassModel;
import io.apicurio.umg.models.java.JavaEntityModel;
import io.apicurio.umg.models.java.JavaPackageModel;
import io.apicurio.umg.pipe.AbstractStage;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Creates the i/o reader classes.  There is a bespoke reader for each specification
 * version.
 * @author eric.wittmann@gmail.com
 */
public class CreateReadersStage extends AbstractStage {

    @Override
    protected void doProcess() {
        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVersion -> {
            String readerPackageName = specVersion.getNamespace() + ".io";
            String readerClassName = specVersion.getPrefix() + "ModelReader";
            // Create the package for the reader
            JavaPackageModel readerPackage = getState().getJavaIndex().lookupAndIndexPackage(() -> {
                JavaPackageModel parentPackage = getState().getJavaIndex().lookupPackage(specVersion.getNamespace());
                JavaPackageModel packageModel = JavaPackageModel.builder()
                        .name(readerPackageName)
                        .parent(parentPackage)
                        .build();
                return packageModel;
            });

            // Create the reader class model
            JavaClassModel readerClass = JavaClassModel.builder()
                    ._package(readerPackage)
                    ._abstract(false)
                    .name(readerClassName)
                    .build();

            // Create java source code for the reader
            JavaClassSource readerClassSource = Roaster.create(JavaClassSource.class)
                    .setPackage(readerClass.get_package().getName())
                    .setName(readerClass.getName())
                    .setAbstract(readerClass.is_abstract())
                    .setPublic();
            readerClassSource.addImport(getState().getConfig().getRootNamespace() + ".util." + "JsonUtil");

            // Create the readXYZ methods - one for each entity
            createReadMethods(specVersion, readerClassSource);

            readerClass.setClassSource(readerClassSource);
            getState().getJavaIndex().addClass(readerClass);
        });
    }

    /**
     * Creates a "read" method for each entity in the spec version.
     * @param specVersion
     * @param readerClassSource
     */
    private void createReadMethods(SpecificationVersion specVersion, JavaClassSource readerClassSource) {
        specVersion.getEntities().forEach(entity -> {
            EntityModel entityModel = getState().getConceptIndex().lookupEntity(specVersion.getNamespace() + "." + entity.getName());
            if (entityModel == null) {
                Logger.warn("[CreateReadersStage] Entity model not found for entity: " + entity);
            } else {
                createReadMethodFor(specVersion, readerClassSource, entityModel);
            }
        });
    }

    /**
     * Creates a single "readXyx" method for the given entity.
     * @param specVersion
     * @param readerClassSource
     * @param entityModel
     */
    private void createReadMethodFor(SpecificationVersion specVersion, JavaClassSource readerClassSource, EntityModel entityModel) {
        String readMethodName = readMethodName(entityModel);

        JavaEntityModel javaEntityModel = getState().getJavaIndex().lookupType(entityModel);
        if (javaEntityModel == null) {
            Logger.warn("[CreateReadersStage] Java entity not found for: " + entityModel.fullyQualifiedName());
            return;
        }

        MethodSource<JavaClassSource> methodSource = readerClassSource.addMethod()
                .setName(readMethodName)
                .setReturnTypeVoid()
                .setPublic();
        methodSource.addParameter(ObjectNode.class, "json");
        methodSource.addParameter(javaEntityModel.getJavaSource().getQualifiedName(), "node");

        // Now create the body content for the reader.
        BodyBuilder body = new BodyBuilder();
        // Read each property of the entity
        Collection<PropertyModel> allProperties = getAllPropertiesFor(entityModel);
        allProperties.forEach(property -> {
            createReadPropertyCode(body, property, entityModel, javaEntityModel, readerClassSource);
        });
        // Read "extra" properties (whatever is left over)
        // TODO read extra properties

        methodSource.setBody(body.toString());
    }

    /**
     * Generates the right java code for reading a single property of an entity.
     * @param body
     * @param property
     * @param javaEntityModel
     * @param javaEntityModel
     * @param readerClassSource
     */
    private void createReadPropertyCode(BodyBuilder body, PropertyModel property, EntityModel entityModel,
            JavaEntityModel javaEntityModel, JavaClassSource readerClassSource) {
        CreateReadProperty crp = new CreateReadProperty(property, entityModel, javaEntityModel, readerClassSource);
        crp.writeTo(body);
    }

    /**
     * Gets a list of all properties for the given entity.  This includes any inherited properties.
     * @param entityModel
     */
    private Collection<PropertyModel> getAllPropertiesFor(EntityModel entityModel) {
        EntityModel model = entityModel;
        Set<PropertyModel> models = new HashSet<>();
        while (model != null) {
            models.addAll(model.getProperties().values());

            // Also include properties from all traits.
            model.getTraits().forEach(trait -> {
                TraitModel t = trait;
                while (t != null) {
                    models.addAll(t.getProperties().values());
                    t = t.getParent();
                }
            });

            model = model.getParent();
        }
        return models;
    }

    private static String readMethodName(EntityModel entityModel) {
        return readMethodName(entityModel.getName());
    }

    private static String createMethodName(EntityModel entityModel) {
        return createMethodName(entityModel.getName());
    }

    @SuppressWarnings("unused")
    private static String addMethodName(EntityModel entityModel) {
        return addMethodName(entityModel.getName());
    }

    private static String readMethodName(String entityName) {
        return "read" + StringUtils.capitalize(entityName);
    }

    private static String createMethodName(String entityName) {
        return "create" + StringUtils.capitalize(entityName);
    }

    private static String addMethodName(String entityName) {
        return "add" + StringUtils.capitalize(entityName);
    }

    @Data
    @AllArgsConstructor
    private class CreateReadProperty {
        PropertyModel property;
        EntityModel entityModel;
        JavaEntityModel javaEntityModel;
        JavaClassSource readerClassSource;

        /**
         * Generates code to read a property from a JSON node into the data model.
         * @param body
         */
        public void writeTo(BodyBuilder body) {
            if ("*".equals(property.getName())) {
                /* Handle mapped properties */
                handleStarProperty(body);
            } else if (property.getName().startsWith("/")) {
                /* Handle regex properties */
                handleRegexProperty(body);
            } else if (property.getType().isEntityType()) {
                handleEntityProperty(body);
            } else if (property.getType().isPrimitiveType()) {
                handlePrimitiveTypeProperty(body);
            } else if (property.getType().isList()) {
                /* Handle List property */
                handleListProperty(body);
            } else if (property.getType().isMap()) {
                /* Handle map property */
                handleMapProperty(body);
            } else if (property.getType().isUnion()) {
                /* Handle union property */
                handleUnionProperty(body);
            } else {
                Logger.warn("[CreateReadersStage] Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
            }
        }

        private void handleStarProperty(BodyBuilder body) {
            if (property.getType().isEntityType()) {
                String entityTypeName = entityModel.getNamespace().fullName() + "." + property.getType().getSimpleType();
                EntityModel propertyTypeEntity = getState().getConceptIndex().lookupEntity(entityTypeName);
                if (propertyTypeEntity == null) {
                    Logger.warn("[CreateReadersStage] STAR Property entity type not found for property: '" + property.getName() + "' of entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type: " + property.getType());
                    return;
                }
                JavaEntityModel propertyTypeJavaModel = getState().getJavaIndex().lookupType(propertyTypeEntity);
                if (propertyTypeJavaModel == null) {
                    Logger.warn("[CreateReadersStage] STAR Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in JAVA index: " + property.getType());
                    return;
                }
                readerClassSource.addImport(propertyTypeJavaModel.fullyQualifiedName());
                readerClassSource.addImport(List.class);

                body.addContext("entityJavaType", propertyTypeJavaModel.getName());
                body.addContext("createMethodName", createMethodName(propertyTypeEntity));
                body.addContext("readMethodName", readMethodName(propertyTypeEntity));
                body.addContext("addMethodName", "addItem");

                body.append("{");
                body.append("    List<String> propertyNames = JsonUtil.keys(json);");
                body.append("    propertyNames.forEach(name -> {");
                body.append("        ObjectNode object = JsonUtil.consumeJsonProperty(json, name);");
                body.append("        ${entityJavaType} model = node.${createMethodName}(name);");
                body.append("        this.${readMethodName}(object, model);");
                body.append("        node.${addMethodName}(name, model);");
                body.append("    });");
                body.append("}");
            } else if (property.getType().isPrimitiveType()) {
                readerClassSource.addImport(List.class);

                body.addContext("valueType", determineValueType(property.getType()));
                body.addContext("consumeProperty", determineConsumePropertyVariant(property.getType()));

                body.append("{");
                body.append("    List<String> propertyNames = JsonUtil.keys(json);");
                body.append("    propertyNames.forEach(name -> {");
                body.append("        ${valueType} value = JsonUtil.${consumeProperty}(json, name);");
                body.append("        node.addItem(name, value);");
                body.append("    });");
                body.append("}");
            } else if (property.getType().isList()) {
                readerClassSource.addImport(List.class);

                PropertyType listValuePropertyType = property.getType().getNested().iterator().next();
                if (listValuePropertyType.getSimpleType().equals("string")) {
                    body.append("{");
                    body.append("    List<String> propertyNames = JsonUtil.keys(json);");
                    body.append("    propertyNames.forEach(name -> {");
                    body.append("        List<String> value = JsonUtil.consumeStringArrayProperty(json, name);");
                    body.append("        node.addItem(name, value);");
                    body.append("    });");
                    body.append("}");
                } else {
                    Logger.warn("[CreateReadersStage] (list) STAR Entity property '" + property.getName() + "' not read (unhandled) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type: " + property.getType());
                }
            } else {
                Logger.warn("[CreateReadersStage] STAR Entity property '" + property.getName() + "' not read (unhandled) for entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
            }
        }

        private void handleRegexProperty(BodyBuilder body) {
            if (property.getType().isEntityType()) {
                String entityTypeName = entityModel.getNamespace().fullName() + "." + property.getType().getSimpleType();
                EntityModel propertyTypeEntity = getState().getConceptIndex().lookupEntity(entityTypeName);
                if (propertyTypeEntity == null) {
                    Logger.warn("[CreateReadersStage] REGEX Property entity type not found for property: '" + property.getName() + "' of entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type: " + property.getType());
                    return;
                }
                JavaEntityModel propertyTypeJavaModel = getState().getJavaIndex().lookupType(propertyTypeEntity);
                if (propertyTypeJavaModel == null) {
                    Logger.warn("[CreateReadersStage] REGEX Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in JAVA index: " + property.getType());
                    return;
                }
                readerClassSource.addImport(propertyTypeJavaModel.fullyQualifiedName());
                readerClassSource.addImport(List.class);

                body.addContext("propertyRegex", encodeRegex(property.getName()));
                body.addContext("entityJavaType", propertyTypeJavaModel.getName());
                body.addContext("createMethodName", createMethodName(propertyTypeEntity));
                body.addContext("readMethodName", readMethodName(propertyTypeEntity));
                body.addContext("addMethodName", addMethodName(property.getCollection()));

                body.append("{");
                body.append("    List<String> propertyNames = JsonUtil.matchingKeys(\"${propertyRegex}\", json);");
                body.append("    propertyNames.forEach(name -> {");
                body.append("        ObjectNode object = JsonUtil.consumeJsonProperty(json, name);");
                body.append("        ${entityJavaType} model = node.${createMethodName}(name);");
                body.append("        this.${readMethodName}(object, model);");
                body.append("        node.${addMethodName}(name, model);");
                body.append("    });");
                body.append("}");
            } else {
                Logger.warn("[CreateReadersStage] REGEX Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
            }
        }

        private void handleEntityProperty(BodyBuilder body) {
            String propertyTypeEntityName = entityModel.getNamespace().fullName() + "." + property.getType().getSimpleType();
            EntityModel propertyTypeEntity = getState().getConceptIndex().lookupEntity(propertyTypeEntityName);
            if (propertyTypeEntity == null) {
                Logger.warn("[CreateReadersStage] Property entity type not found for property: '" + property.getName() + "' of entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
                return;
            }

            body.addContext("propertyName", property.getName());
            body.addContext("setterMethodName", Util.fieldSetter(property));
            body.addContext("createMethodName", createMethodName(propertyTypeEntity));
            body.addContext("getterMethodName", Util.fieldGetter(property));
            body.addContext("readMethodName", readMethodName(propertyTypeEntity));

            body.append("{");
            body.append("    ObjectNode object = JsonUtil.consumeJsonProperty(json, \"${propertyName}\");");
            body.append("    if (object != null) {");
            body.append("        node.${setterMethodName}(node.${createMethodName}());");
            body.append("        ${readMethodName}(object, node.${getterMethodName}());");
            body.append("    }");
            body.append("}");
        }

        private void handlePrimitiveTypeProperty(BodyBuilder body) {
            body.addContext("valueType", determineValueType(property.getType()));
            body.addContext("consumeProperty", determineConsumePropertyVariant(property.getType()));
            body.addContext("propertyName", property.getName());
            body.addContext("setterMethodName", Util.fieldSetter(property));

            body.append("{");
            body.append("    ${valueType} value = JsonUtil.${consumeProperty}(json, \"${propertyName}\");");
            body.append("    node.${setterMethodName}(value);");
            body.append("}");
        }

        private void handleListProperty(BodyBuilder body) {
            body.addContext("propertyName", property.getName());
            body.addContext("setterMethodName", Util.fieldSetter(property));

            PropertyType listValuePropertyType = property.getType().getNested().iterator().next();
            if (listValuePropertyType.getSimpleType().equals("string")) {
                body.append("{");
                body.append("    List<String> value = JsonUtil.consumeStringArrayProperty(json, \"${propertyName}\");");
                body.append("    node.${setterMethodName}(value);");
                body.append("}");
            } else if (listValuePropertyType.getSimpleType().equals("any")) {
                body.append("{");
                body.append("    Map<String, JsonNode> value = JsonUtil.consumeAnyArrayProperty(json, \"${propertyName}\");");
                body.append("    node.${setterMethodName}(value);");
                body.append("}");
            } else if (listValuePropertyType.isEntityType()) {
                String entityTypeName = listValuePropertyType.getSimpleType();
                String fqEntityName = entityModel.getNamespace().fullName() + "." + entityTypeName;
                EntityModel entityTypeModel = getState().getConceptIndex().lookupEntity(fqEntityName);
                if (entityTypeModel == null) {
                    Logger.warn("[CreateReadersStage] LIST Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in index: " + property.getType());
                    return;
                }
                JavaEntityModel entityTypeJavaModel = getState().getJavaIndex().lookupType(entityTypeModel);
                if (entityTypeJavaModel == null) {
                    Logger.warn("[CreateReadersStage] LIST Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in JAVA index: " + property.getType());
                    return;
                }
                readerClassSource.addImport(entityTypeJavaModel.fullyQualifiedName());
                readerClassSource.addImport(List.class);
                readerClassSource.addImport(ArrayList.class);

                body.addContext("mapValueJavaType", entityTypeJavaModel.getName());
                body.addContext("createMethodName", createMethodName(entityTypeModel));
                body.addContext("readMethodName", readMethodName(entityTypeModel));
                body.addContext("setterMethod", Util.fieldSetter(property));

                body.append("{");
                body.append("    List<ObjectNode> objects = JsonUtil.consumeJsonArrayProperty(json, \"${propertyName}\");");
                body.append("    if (objects != null) {");
                body.append("        List<${mapValueJavaType}> models = new ArrayList<>();");
                body.append("        objects.forEach(object -> {");
                body.append("            ${mapValueJavaType} model = node.${createMethodName}();");
                body.append("            this.${readMethodName}(object, model);");
                body.append("            models.add(model);");
                body.append("        });");
                body.append("        node.${setterMethod}(models);");
                body.append("    }");
                body.append("}");
            } else {
                Logger.warn("[CreateReadersStage] LIST Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
            }
        }

        private void handleMapProperty(BodyBuilder body) {
            body.addContext("propertyName", property.getName());
            body.addContext("setterMethodName", Util.fieldSetter(property));

            PropertyType mapValuePropertyType = property.getType().getNested().iterator().next();
            if (mapValuePropertyType.getSimpleType().equals("string")) {
                body.append("{");
                body.append("    Map<String, String> value = JsonUtil.consumeStringMapProperty(json, \"${propertyName}\");");
                body.append("    node.${setterMethodName}(value);");
                body.append("}");
            } else if (mapValuePropertyType.getSimpleType().equals("any")) {
                body.append("{");
                body.append("    Map<String, JsonNode> value = JsonUtil.consumeAnyMapProperty(json, \"${propertyName}\");");
                body.append("    node.${setterMethodName}(value);");
                body.append("}");
            } else if (mapValuePropertyType.getSimpleType().equals("object")) {
                body.append("{");
                body.append("    Map<String, ObjectNode> value = JsonUtil.consumeJsonMapProperty(json, \"${propertyName}\");");
                body.append("    node.${setterMethodName}(value);");
                body.append("}");
            } else if (mapValuePropertyType.isEntityType()) {
                String entityTypeName = mapValuePropertyType.getSimpleType();
                String fqEntityName = entityModel.getNamespace().fullName() + "." + entityTypeName;
                EntityModel entityTypeModel = getState().getConceptIndex().lookupEntity(fqEntityName);
                if (entityTypeModel == null) {
                    Logger.warn("[CreateReadersStage] MAP Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in index: " + property.getType());
                    return;
                }
                JavaEntityModel entityTypeJavaModel = getState().getJavaIndex().lookupType(entityTypeModel);
                if (entityTypeJavaModel == null) {
                    Logger.warn("[CreateReadersStage] MAP Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                    Logger.warn("[CreateReadersStage]        property type is entity but not found in JAVA index: " + property.getType());
                    return;
                }
                readerClassSource.addImport(entityTypeJavaModel.fullyQualifiedName());

                body.addContext("objectName", "object");
                body.addContext("mapValueJavaType", entityTypeJavaModel.getName());
                body.addContext("createMethodName", "create" + entityTypeName);
                body.addContext("readMethodName", "read" + entityTypeName);
                body.addContext("addMethodName", "add" + entityTypeName);

                body.append("{");
                body.append("    ObjectNode ${objectName} = JsonUtil.consumeJsonProperty(json, \"${propertyName}\");");
                body.append("    JsonUtil.keys(${objectName}).forEach(name -> {");
                body.append("        ObjectNode mapValue = JsonUtil.consumeJsonProperty(${objectName}, name);");
                body.append("        ${mapValueJavaType} model = node.${createMethodName}(name);");
                body.append("        this.${readMethodName}(mapValue, model);");
                body.append("        node.${addMethodName}(name, model);");
                body.append("    });");
                body.append("}");
            } else {
                Logger.warn("[CreateReadersStage] MAP Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
                Logger.warn("[CreateReadersStage]        property type: " + property.getType());
            }
        }

        private void handleUnionProperty(BodyBuilder body) {
            // TODO Auto-generated method stub
            Logger.warn("[CreateReadersStage] UNION Entity property '" + property.getName() + "' not read (unsupported) for entity: " + entityModel.fullyQualifiedName());
            Logger.warn("[CreateReadersStage]        property type: " + property.getType());

        }

        /**
         * Figure out which variant of "consumeProperty" from "JsonUtil" we should use for
         * this property.  The property might be a primitive type, or a list/map of primitive
         * types, or an Entity type, or a list/map of Entity types.
         */
        private String determineConsumePropertyVariant(PropertyType type) {
            if (type.isEntityType()) {
                return "consumeJsonProperty";
            }

            if (type.isPrimitiveType()) {
                Class<?> _class = Util.primitiveTypeToClass(type);
                if (ObjectNode.class.equals(_class)) {
                    return "consumeJsonProperty";
                } else if (JsonNode.class.equals(_class)) {
                    return "consumeAnyProperty";
                } else {
                    return "consume" + _class.getSimpleName() + "Property";
                }
            }

            Logger.warn("[CreateReadersStage] Unable to determine value type for: " + property);
            return "consumeProperty";
        }

        /**
         * Determines the Java data type of the given property.
         */
        private String determineValueType(PropertyType type) {
            if (type.isPrimitiveType()) {
                Class<?> _class = Util.primitiveTypeToClass(type);
                if (_class != null) {
                    return _class.getSimpleName();
                }
            }
            Logger.warn("[CreateReadersStage] Unable to determine value type for: " + property);
            return "Object";
        }

        private String encodeRegex(String regex) {
            return regex.replace("\\", "\\\\");
        }
    }
}
