package io.apicurio.umg.pipe.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.models.java.type.UnionJavaType;
import io.apicurio.umg.pipe.java.method.*;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;

/**
 * Creates the i/o reader classes.  There is a bespoke reader for each specification
 * version.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateWritersStage extends AbstractJavaStage {

    private Type root;

    @Override
    protected void doProcess() {
        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVersion -> {
            createWriter(specVersion);
        });
    }

    /**
     * Creates a reader for the given spec version.
     *
     * @param specVersion
     */
    private void createWriter(SpecificationVersion specVersion) {
        String writerPackageName = getWriterPackageName(specVersion);
        String writerClassName = getWriterClassName(specVersion);

        // Create java source code for the reader
        JavaClassSource writerClassSource = Roaster.create(JavaClassSource.class)
                .setPackage(writerPackageName)
                .setName(writerClassName)
                .setPublic();
        writerClassSource.addImport(getState().getConfig().getRootNamespace() + ".util." + "JsonUtil");
        writerClassSource.addImport(getState().getConfig().getRootNamespace() + ".util." + "WriterUtil");

        // Implements the ModelReader interface
        JavaInterfaceSource modelWriterInterfaceSource = getState().getJavaIndex().lookupInterface(getModelWriterInterfaceFQN());
        writerClassSource.addImport(modelWriterInterfaceSource);
        writerClassSource.addInterface(modelWriterInterfaceSource);

        // Identify the root type
        var root = getState().getConceptIndex().getTypes().stream()
                .filter(t -> t.isType())
                .map(t -> (Type) t)
                .filter(t -> t.getNamespace().equals(specVersion.getNamespace()))
                .filter(t -> t.isRoot())
                .collect(Collectors.toList());
        assertion(root.size() == 1);
        this.root = root.get(0);

        writeMethodForRoot(writerClassSource);

        getState().getConceptIndex().getTypes().stream()
                .filter(t -> !t.isTraitTypeLike() && !t.isPrimitiveType())
                .map(t -> (Type) t)
                .filter(t -> t.getNamespace().equals(specVersion.getNamespace()))
                .forEach(t -> {
                    writeMethodForAnyType(writerClassSource, t);
                });

        getState().getJavaIndex().index(writerClassSource);
    }


    private void writeMethodForRoot(JavaClassSource writerClassSource) {

        var jt = getState().getJavaIndex().requireType(root);
        jt.addImportsTo(writerClassSource);

        var methodSource = writerClassSource.addMethod()
                .setName("writeRoot")
                .setReturnType(ObjectNode.class)
                .setPublic();
        methodSource.addAnnotation(Override.class);

        methodSource.addParameter("RootCapable", "node");
        writerClassSource.addImport(getState().getConfig().getRootNamespace() + ".RootCapable");

        var body = new BodyBuilder();

        body.c("writeMethodName", writeMethodName(jt));
        body.c("valueType", jt.toJavaTypeString(false));

        body.a("if(!(node instanceof ${valueType})) {");
        body.a("throw new IllegalArgumentException(\"Expected ${valueType} but got \" + node != null ? node.getClass().getSimpleName() : \"null\");");
        body.a("}");
        body.a("return (ObjectNode) ${writeMethodName}((${valueType}) node);");

        methodSource.setBody(body.toString());
    }


    private void writeMethodForAnyType(JavaClassSource writerClassSource, Type t) {

        if (t.isEntityType()) {
            writeMethodForEntityType(writerClassSource, (EntityType) t);
        } else if (t.isUnionType()) {
            writeMethodForUnionType(writerClassSource, (UnionType) t);
        } else if (t.isListType()) {
            writeMethodForListType(writerClassSource, (ListType) t);
        } else if (t.isMapType()) {
            writeMethodForMapType(writerClassSource, (MapType) t);
        } else {
            fail("TODO");
        }
    }


    private void writeMethodForEntityType(JavaClassSource writerClassSource, EntityType t) {

        var jt = (EntityJavaType) getState().getJavaIndex().requireType(t);
        jt.addImportsTo(writerClassSource);

        // We'll generate two methods for an entity:
        // #1 Base write method: writeX(Node node, ObjectNode json);
        // #2 No node version: writeX(Node node);

        // Generate #2
        {
            var noJsonMethodSource = writerClassSource.addMethod()
                    .setName(writeMethodName(jt))
                    .setReturnType(ObjectNode.class.getSimpleName())
                    .setPublic();

            noJsonMethodSource.addParameter(jt.getInterfaceSource().getName(), "node");

            var body = new BodyBuilder();

            body.c("writeMethodName", writeMethodName(jt));

            body.a("if(node == null) {").indent();
            body.a("return null;");
            body.deindent().a("}");
            body.a("ObjectNode json = JsonUtil.objectNode();");
            body.a("${writeMethodName}(node, json);");
            body.a("return json;");

            noJsonMethodSource.setBody(body.toString());
        }

        // Generate #1

        var methodSource = writerClassSource.addMethod()
                .setName(writeMethodName(jt))
                .setReturnTypeVoid()
                .setPublic();

        writerClassSource.addImport(ObjectNode.class);
        methodSource.addParameter(jt.getInterfaceSource().getName(), "node");
        methodSource.addParameter(ObjectNode.class.getSimpleName(), "json");

        var body = new BodyBuilder();

        body.a("if(node == null || json == null) {").indent();
        body.a("return;");
        body.deindent().a("}");

        var properties = new ArrayList<PropertyModel>();
        properties.addAll(t.getEntity().getProperties().values());
        properties.addAll(t.getEntity().getTraits().stream().flatMap(tt -> tt.getProperties().values().stream()).collect(Collectors.toSet()));

        properties.forEach(p -> {

            var jpt = getState().getJavaIndex().requireType(p.getType());
            jpt.addImportsTo(writerClassSource);


            body.c("valueType", jpt.toJavaTypeString(false));
            body.c("getterMethodName", GetterMethod.methodName(p, jpt));

            // Return values from some getters have to be cast
            var suppressWarnings = false;
            if (!JavaUtils.isOldestParent(getState(), jpt)) {
                body.c("cast", "(${valueType}) " + (p.getType().isCollectionType() ? "(Object) " : ""));
                if (p.getType().isCollectionType()) {
                    writerClassSource.addImport(SuppressWarnings.class);
                    suppressWarnings = true;
                }
            } else {
                body.c("cast", "");
            }

            // PREFIX CODE
            body.a("{");
            body.indent();

            // GET VALUE
            if (p.isStar()) {

                body.c("propertyName", "name");
                body.c("propertyValue", "value");

                body.a("List<String> nameList = node.getItemNames();");
                body.a("nameList.forEach(name -> {");
                body.indent();
                if (suppressWarnings) {
                    body.a("@SuppressWarnings(\"unchecked\")");
                }
                body.a("${valueType} value = ${cast}node.getItem(name);");

            } else if (p.isRegex()) {

                writerClassSource.addImport(Entry.class);

                body.c("propertyName", "k");
                body.c("propertyValue", "v");

                if (suppressWarnings) {
                    body.a("@SuppressWarnings(\"unchecked\")");
                }
                body.a("Map<String, ${valueType}> valueMap = ${cast}node.${getterMethodName}();");
                body.a("valueMap.forEach((k,v) -> {");
                body.indent();

            } else {

                body.c("propertyName", "\"" + p.getName() + "\"");
                body.c("propertyValue", "value");

                if (suppressWarnings) {
                    body.a("@SuppressWarnings(\"unchecked\")");
                }
                body.a("${valueType} value = ${cast}node.${getterMethodName}();");
            }

            // Null check
            body.a("if (${propertyValue} != null) {").indent();

            // WRITING

            if (p.getType().isPrimitiveType()) {

                body.c("nodeMethod", determinePrimitiveNodeVariant((PrimitiveType) p.getType(), body.getContext("propertyValue")));

                body.a("JsonNode j = ${nodeMethod};");

            } else {

                body.c("writeMethodName", writeMethodName(jpt));
                body.a("JsonNode j = ${writeMethodName}(${propertyValue});");

            }

            //body.a("if (j != null) {").indent();
            body.a("JsonUtil.objectPut(json, ${propertyName}, j);");
            //body.deindent().a("}");
            body.deindent().a("}");

            // POSTFIX CODE
            if (p.isStar() || p.isRegex()) {

                body
                        .deindent().a("});");
            }

            body.deindent().a("}");
        });

        createWriteExtraPropertiesCode(body);

        methodSource.setBody(body.toString());
    }


    private void writeMethodForUnionType(JavaClassSource writerClassSource, UnionType t) {

        var jt = (UnionJavaType) getState().getJavaIndex().requireType(t);

        var methodSource = writerClassSource.addMethod()
                .setName(writeMethodName(jt))
                .setReturnType(JsonNode.class.getSimpleName())
                .setPublic();

        methodSource.addParameter(jt.getInterfaceSource().getName(), "node");
        jt.addImportsTo(writerClassSource);

        var body = new BodyBuilder();

        body.a("if(node == null) {").indent();
        body.a("return null;");
        body.deindent().a("}");

        t.getTypes().forEach(nt -> {
            var jnt = getState().getJavaIndex().requireType(nt);
            jnt.addImportsTo(writerClassSource);

            var oldestParent = jnt;
            while (oldestParent.getTypeModel().getParent() != null) {
                oldestParent = getState().getJavaIndex().requireType(oldestParent.getTypeModel().getParent());
            }

            body.c("isMethodName", UnionIsMethod.methodName(oldestParent));
            body.c("asMethodName", UnionAsMethod.methodName(oldestParent));
            body.c("valueType", jnt.toJavaTypeString(false));

            body.a("if(node.${isMethodName}()) {").indent();
            body.a("${valueType} value = node.${asMethodName}();");

            if (nt.isPrimitiveType()) {

                body.c("nodeMethod", determinePrimitiveNodeVariant((PrimitiveType) nt, "value"));
                body.a("return ${nodeMethod};");

            } else {

                body.c("writeMethodName", writeMethodName(jnt));
                body.a("return ${writeMethodName}(value);");
            }

            body.deindent().a("} else ");
        });

        body.a("{");
        body.a("    return null;");
        body.a("}");

        methodSource.setBody(body.toString());
    }

    private void writeMethodForListType(JavaClassSource readerClassSource, ListType t) {

        var jt = getState().getJavaIndex().requireType(t);
        jt.addImportsTo(readerClassSource);
        var vt = t.getValueType();
        var jvt = getState().getJavaIndex().requireType(vt);
        jvt.addImportsTo(readerClassSource);

        var methodSource = readerClassSource.addMethod()
                .setName(writeMethodName(jt))
                .setReturnType(JsonNode.class.getSimpleName())
                .setPublic();

        methodSource.addParameter(jt.toJavaTypeString(false), "value");

        var body = new BodyBuilder();

        body.c("valueType", jvt.toJavaTypeString(false));

        // PREFIX
        readerClassSource.addImport(ArrayNode.class);

        body.a("if (value == null) {").indent();
        body.a("return null;");
        body.deindent().a("}");

        body.a("ArrayNode json = JsonUtil.arrayNode();");
        body.a("value.forEach(v -> {").indent();

        if (vt.isPrimitiveType()) {

            body.c("nodeMethod", determinePrimitiveNodeVariant((PrimitiveType) vt, "v"));

            body.a("JsonNode j = ${nodeMethod};");

        } else {

            body.c("writeMethodName", writeMethodName(jvt));
            body.a("JsonNode j = ${writeMethodName}(v);");
        }

        // POSTFIX

        body.a("JsonUtil.arrayAdd(json, j);");
        body.deindent().a("});");
        body.a("return json;");

        methodSource.setBody(body.toString());
    }

    private void writeMethodForMapType(JavaClassSource readerClassSource, MapType t) {

        var jt = getState().getJavaIndex().requireType(t);
        jt.addImportsTo(readerClassSource);
        var vt = t.getValueType();
        var jvt = getState().getJavaIndex().requireType(vt);
        jvt.addImportsTo(readerClassSource);

        var methodSource = readerClassSource.addMethod()
                .setName(writeMethodName(jt))
                .setReturnType(JsonNode.class.getSimpleName())
                .setPublic();

        methodSource.addParameter(jt.toJavaTypeString(false), "value");

        var body = new BodyBuilder();

        body.c("valueType", jvt.toJavaTypeString(false));

        // PREFIX
        readerClassSource.addImport(ObjectNode.class);

        body.a("if (value == null) {").indent();
        body.a("return null;");
        body.deindent().a("}");

        body.a("ObjectNode json = JsonUtil.objectNode();");
        body.a("value.forEach((k, v) -> {").indent();

        if (vt.isPrimitiveType()) {

            body.c("nodeMethod", determinePrimitiveNodeVariant((PrimitiveType) vt, "v"));

            body.a("JsonNode j = ${nodeMethod};");

        } else {

            body.c("writeMethodName", writeMethodName(jvt));
            body.a("JsonNode j = ${writeMethodName}(v);");
        }

        // POSTFIX

        body.a("JsonUtil.objectPut(json, k, j);");
        body.deindent().a("});");
        body.a("return json;");

        methodSource.setBody(body.toString());
    }


    /**
     * Creates code that will read any extra/remaining properties on a JSON object.
     *
     * @param body
     */
    private void createWriteExtraPropertiesCode(BodyBuilder body) {
        body.append("WriterUtil.writeExtraProperties(node, json);");
    }


    private static String determinePrimitiveNodeVariant(PrimitiveType type, String variable) {
        Class<?> _class = type.get_class();
        if (ObjectNode.class.equals(_class)) {
            return variable;
        } else if (JsonNode.class.equals(_class)) {
            return variable;
        } else if (String.class.equals(_class)) {
            return "JsonUtil.textNode(" + variable + ")";
        } else if (Boolean.class.equals(_class)) {
            return "JsonUtil.booleanNode(" + variable + ")";
        } else if (Number.class.equals(_class)) {
            return "JsonUtil.numericNode(" + variable + ")";
        } else if (Integer.class.equals(_class)) {
            return "JsonUtil.numericNode(" + variable + ")";
        } else {
            fail("Unexpected primitive type: %s", type);
            return null; // Unreachable
        }
    }


    private static String determineSetMethodVariant(PrimitiveType type) {
        Class<?> _class = type.get_class();
        if (ObjectNode.class.equals(_class)) {
            return "JsonUtil.setAnyProperty";
        } else if (JsonNode.class.equals(_class)) {
            return "JsonUtil.setAnyProperty";
        } else {
            return "JsonUtil.set" + _class.getSimpleName() + "Property";
        }
    }


    private static String writeMethodName(IJavaType javaType) {
        return "write" + StringUtils.capitalize(javaType.getName(false, false));
    }
}
