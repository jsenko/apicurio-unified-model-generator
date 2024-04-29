package io.apicurio.umg.pipe.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.models.concept.ConceptUtils;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.*;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.models.java.type.UnionJavaType;
import io.apicurio.umg.pipe.java.method.BodyBuilder;
import io.apicurio.umg.pipe.java.method.SetterMethod;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;
import static io.apicurio.umg.pipe.java.method.BodyBuilder.escapeJavaString;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Creates the i/o reader classes.  There is a bespoke reader for each specification
 * version.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateReadersStage extends AbstractJavaStage {

    private Type root;

    private Set<Type> rootReachable;

    @Override
    protected void doProcess() {
        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVersion -> {
            createReader(specVersion);
        });
    }

    /**
     * Creates a reader for the given spec version.
     *
     * @param specVersion
     */
    private void createReader(SpecificationVersion specVersion) {
        String readerPackageName = getReaderPackageName(specVersion);
        String readerClassName = getReaderClassName(specVersion);

        // Create java source code for the reader
        JavaClassSource readerClassSource = Roaster.create(JavaClassSource.class)
                .setPackage(readerPackageName)
                .setName(readerClassName)
                .setPublic();
        readerClassSource.addImport(getState().getConfig().getRootNamespace() + ".util." + "JsonUtil");
        readerClassSource.addImport(getState().getConfig().getRootNamespace() + ".util." + "ReaderUtil");

        // Implements the ModelReader interface
        JavaInterfaceSource modelReaderInterfaceSource = getState().getJavaIndex().lookupInterface(getModelReaderInterfaceFQN());
        readerClassSource.addImport(modelReaderInterfaceSource);
        readerClassSource.addInterface(modelReaderInterfaceSource);

        // Identify the root type
        var root = getState().getConceptIndex().getTypes().stream()
                .filter(t -> t.isType())
                .map(t -> (Type) t)
                .filter(t -> t.getNamespace().equals(specVersion.getNamespace()))
                .filter(t -> t.isRoot())
                .collect(Collectors.toList());
        assertion(root.size() == 1);
        this.root = root.get(0);

        this.rootReachable = ConceptUtils.collectNestedTypes(this.root).stream()
                .filter(t -> t.isType()) // TODO: Should not be necessary.
                .map(t -> (Type) t)
                .collect(Collectors.toSet());

        readMethodForRoot(readerClassSource);

        getState().getConceptIndex().getTypes().stream()
                .filter(t -> !t.isTraitTypeLike() && !t.isPrimitiveType())
                .map(t -> (Type) t)
                .filter(t -> t.getNamespace().equals(specVersion.getNamespace()))
                .forEach(t -> {
                    readMethodForAnyType(readerClassSource, t);
                });

        getState().getJavaIndex().index(readerClassSource);
    }


    private void readMethodForRoot(JavaClassSource readerClassSource) {

        var jt = getState().getJavaIndex().requireType(root);
        jt.addImportsTo(readerClassSource);

        var methodSource = readerClassSource.addMethod()
                .setName("readRoot")
                .setReturnType(jt.toJavaTypeString(false))
                .setPublic();
        methodSource.addAnnotation(Override.class);

        methodSource.addParameter(ObjectNode.class.getSimpleName(), "json");

        var body = new BodyBuilder();

        body.c("readMethodName", readMethodName(jt.getName(false, false)));

        //body.a("if (!JsonUtil.isObject(json)) {");
        //body.a("    return null;");
        //body.a("}");
        body.a("return ${readMethodName}(json, true);");

        methodSource.setBody(body.toString());
    }


    private void readMethodForAnyType(JavaClassSource readerClassSource, Type t) {

        if (t.isEntityType()) {
            readMethodForEntityType(readerClassSource, (EntityType) t);
        } else if (t.isUnionType()) {
            readMethodForUnionType(readerClassSource, (UnionType) t);
        } else if (t.isListType()) {
            readMethodForListType(readerClassSource, (ListType) t);
        } else if (t.isMapType()) {
            readMethodForMapType(readerClassSource, (MapType) t);
        } else {
            fail("TODO");
        }
    }


    private void readMethodForEntityType(JavaClassSource readerClassSource, EntityType t) {

        var jt = (EntityJavaType) getState().getJavaIndex().requireType(t);
        jt.addImportsTo(readerClassSource);

        // We'll generate up to three methods for an entity:
        // #1 Base read method: readX(ObjectNode json, Node node);
        // #2 No node version: readX(JsonNode json);
        // #3 No node root-aware version (if needed): readX(JsonNode json, boolean isRoot);
        // We need to generate #1 and #2 separately for backwards compatibility, to support read dispatchers.

        // Generate #2
        // If this type is part of root, we'll generate an extra method for a non-root case
        if (rootReachable.contains(t)) {
            var noNodeNoRootMethodSource = readerClassSource.addMethod()
                    .setName(readMethodName(jt.getName(false, false)))
                    .setReturnType(jt.getInterfaceSource())
                    .setPublic();

            readerClassSource.addImport(JsonNode.class);
            noNodeNoRootMethodSource.addParameter(JsonNode.class.getSimpleName(), "json");

            var body = new BodyBuilder();

            body.c("readMethodName", readMethodName(jt.getName(false, false)));

            body.a("return ${readMethodName}(json, false);");

            noNodeNoRootMethodSource.setBody(body.toString());
        }

        // Generate either #2 or #3
        {
            var noNodeMethodSource = readerClassSource.addMethod()
                    .setName(readMethodName(jt.getName(false, false)))
                    .setReturnType(jt.getInterfaceSource())
                    .setVisibility(rootReachable.contains(t) ? Visibility.PRIVATE : Visibility.PUBLIC);

            readerClassSource.addImport(JsonNode.class);
            noNodeMethodSource.addParameter(JsonNode.class.getSimpleName(), "json");
            if (rootReachable.contains(t)) {
                noNodeMethodSource.addParameter(boolean.class, "isRoot");
            }

            var body = new BodyBuilder();

            body.c("readMethodName", readMethodName(jt.getName(false, false)));

            body
                    .a("if (!JsonUtil.isObject(json)) {")
                    .indent().a("return null;")
                    .deindent().a("}");

            body.c("nodeType", jt.getInterfaceSource().getName());
            body.c("nodeTypeImpl", jt.getClassSource().getName());

            if (rootReachable.contains(t)) {
                body.a("${nodeType} node = isRoot ? ${nodeType}.createRoot() : new ${nodeTypeImpl}();");
            } else {
                body.a("${nodeType} node = new ${nodeTypeImpl}();");
            }
            body.a("return ${readMethodName}(JsonUtil.toObject(json), node);");
            noNodeMethodSource.setBody(body.toString());
        }
        // Generate #1

        var methodSource = readerClassSource.addMethod()
                .setName(readMethodName(jt.getName(false, false)))
                .setReturnType(jt.getInterfaceSource())
                .setPublic();

        readerClassSource.addImport(ObjectNode.class);
        methodSource.addParameter(ObjectNode.class.getSimpleName(), "json");
        methodSource.addParameter(jt.getInterfaceSource().getName(), "node");

        var body = new BodyBuilder();

        // CREATE NODE

        var properties = new ArrayList<PropertyModel>();
        properties.addAll(t.getEntity().getProperties().values());
        properties.addAll(t.getEntity().getTraits().stream().flatMap(tt -> tt.getProperties().values().stream()).collect(Collectors.toSet()));

        properties.forEach(p -> {

            var jpt = getState().getJavaIndex().requireType(p.getType());
            jpt.addImportsTo(readerClassSource);

            body.c("valueType", jpt.toJavaTypeString(false));

            // PREFIX CODE
            body.a("{");
            body.indent();

            if (p.isStar()) {

                body.c("addMethodName", "addItem");

                body.a("List<String> propertyNames = JsonUtil.keys(json);");
                body.a("propertyNames.forEach(name -> {");
                body.indent();
                body.a("JsonNode any = JsonUtil.consumeAnyProperty(json, name);");
                body.a("if (any != null) {");
                body.indent();

            } else if (p.isRegex()) {

                body.c("propertyRegex", encodeRegex(extractRegex(p.getName())));
                body.c("addMethodName", addMethodName(singularize(p.getCollection())));

                body.a("List<String> propertyNames = JsonUtil.matchingKeys(\"${propertyRegex}\", json);");
                body.a("propertyNames.forEach(name -> {");
                body.indent();
                body.a("JsonNode any = JsonUtil.consumeAnyProperty(json, name);");
                body.a("if (any != null) {");
                body.indent();

            } else {

                body.c("propertyName", p.getName());
                body.c("addMethodName", addMethodName(singularize(p.getName())));

                body.a("JsonNode any = JsonUtil.consumeAnyProperty(json, \"${propertyName}\");");
                body.a("if (any != null) {");
                body.indent();
            }

            body.c("propertyType", jpt.toJavaTypeString(false));
            body.c("setterMethodName", SetterMethod.methodName(p));

            // READING
            if (p.getType().isPrimitiveType()) {

                body.c("toMethod", determineToVariant((PrimitiveType) p.getType(), "any"));

                body.a("${valueType} value = ${toMethod};");
                body.a("if(value != null) {");
                body.indent();

            } else {

                body.c("readMethodName", readMethodName(jpt.getName(false, false)));

                body.a("${valueType} value = ${readMethodName}(JsonUtil.clone(any));");
                body.a("if(value != null) {");
                body.indent();

            }

            // HANDLE COLLECTION ITERATION

            if (p.getType().isListType()) {

                body.c("valueVar", "v");

                body.a("value.forEach(v -> {");
                body.indent();

            } else if (p.getType().isMapType()) {

                body.c("valueVar", "v");

                body.a("value.forEach((k,v) -> {");
                body.indent();

            } else {
                // Do nothing
                body.c("valueVar", "value");
            }

            // ATTACH
            var needsAttach = ConceptUtils.collectNestedTypes(p.getType()).stream().anyMatch(tt -> tt.isEntityType());
            if (needsAttach) {
                readerClassSource.addImport("io.apicurio.datamodels.models.Any"); // TODO
                body.a("Any.attach(${valueVar}, node);");
            }

            // SET/ADD CODE
            body.c("setterMethodName", setterMethodName(p));

            if (p.isStar() || p.isRegex()) {

                if (p.getType().isCollectionType()) {
                    body.deindent().a("});");
                }

                body.a("node.${addMethodName}(name, value);");

            } else {
                if (p.getType().isListType()) {

                    body
                            .a("node.${addMethodName}(v);")
                            .deindent().a("});");

                } else if (p.getType().isMapType()) {

                    body
                            .a("node.${addMethodName}(k, v);")
                            .deindent().a("});");

                } else {

                    body.a("node.${setterMethodName}(value);");
                }
            }

            // POSTFIX CODE
            if (p.isStar() || p.isRegex()) {

                body
                        .deindent().a("} else {")
                        .indent().a("node.addExtraProperty(name, any);")
                        .deindent().a("}")
                        .deindent().a("}")
                        .deindent().a("});");

            } else {


                body
                        .deindent().a("} else {")
                        .indent().a("node.addExtraProperty(\"${propertyName}\", any);")
                        .deindent().a("}")
                        .deindent().a("}");
            }

            body.deindent().a("}");
        });

        createReadExtraPropertiesCode(body);

        body.a("return node;");

        methodSource.setBody(body.toString());
    }


    private void readMethodForUnionType(JavaClassSource readerClassSource, UnionType t) {

        var jt = (UnionJavaType) getState().getJavaIndex().requireType(t);

        if (rootReachable.contains(t)) {
            var nonrootMethodSource = readerClassSource.addMethod()
                    .setName(readMethodName(jt.getName(false, false)))
                    .setReturnType(jt.getInterfaceSource())
                    .setPublic();

            readerClassSource.addImport(JsonNode.class);
            nonrootMethodSource.addParameter(JsonNode.class.getSimpleName(), "json");

            var body = new BodyBuilder();

            body.c("readMethodName", readMethodName(jt.getName(false, false)));

            body.a("return ${readMethodName}(json, false);");

            nonrootMethodSource.setBody(body.toString());
        }

        var methodSource = readerClassSource.addMethod()
                .setName(readMethodName(jt.getName(false, false)))
                .setReturnType(jt.getInterfaceSource())
                .setVisibility(rootReachable.contains(t) ? Visibility.PRIVATE : Visibility.PUBLIC);

        readerClassSource.addImport(ObjectNode.class);
        methodSource.addParameter(JsonNode.class.getSimpleName(), "json");
        if (rootReachable.contains(t)) {
            methodSource.addParameter(boolean.class, "isRoot");
        }

        var body = new BodyBuilder();

        t.getTypes().forEach(nt -> {
            var jnt = getState().getJavaIndex().requireType(nt);
            jnt.addImportsTo(readerClassSource);
            var rule = t.getRuleFor(nt.getName());
            switch (rule.getRuleType()) {
                case IsJsonTypes: {
                    var types = rule.getJsonTypes();
                    requireNonNull(types);
                    var cond = new ArrayList<String>();
                    types.forEach(tt -> {
                        if (!List.of("object", "array", "string", "boolean", "number").contains(tt)) {
                            throw new RuntimeException("Illegal union rule jsonType: " + tt);
                        }
                        cond.add("JsonUtil.is" + capitalize(tt) + "(json)");
                    });
                    body.a("if (" + String.join(" || ", cond) + ") {");
                }
                break;
                case IsJsonValue: {
                    var type = rule.getJsonType();
                    requireNonNull(type);
                    if (!List.of("object", "array", "string", "boolean", "number").contains(type)) {
                        throw new RuntimeException("Illegal union rule jsonType: " + type);
                    }
                    body.c("type", capitalize(type));
                    var value = rule.getJsonValue();
                    requireNonNull(value);
                    body.a("if (JsonUtil.is${type}(json) && JsonUtil.equals(json, JsonUtil.parse${type}(" + escapeJavaString(value) + "))) {");
                }
                break;
                case IsJsonObjectWithPropertyName: {
                    var propertyName = rule.getName();
                    requireNonNull(propertyName);
                    body.c("propertyName", propertyName);
                    body.a("if (JsonUtil.isObjectWithProperty(json, \"${propertyName}\")) {");
                }
                break;
                case IsJsonObjectWithoutPropertyName: {
                    var propertyName = rule.getName();
                    requireNonNull(propertyName);
                    body.c("propertyName", propertyName);
                    body.a("if (!JsonUtil.isObjectWithProperty(json, \"${propertyName}\")) {");
                }
                break;
                case IsJsonObjectWithPropertyTypes: {
                    var propertyName = rule.getName();
                    requireNonNull(propertyName);
                    body.c("propertyName", propertyName);
                    var types = rule.getJsonTypes();
                    requireNonNull(types);
                    var cond = new ArrayList<String>();
                    types.forEach(tt -> {
                        if (!List.of("object", "array", "string", "boolean", "number").contains(tt)) {
                            throw new RuntimeException("Illegal union rule jsonType: " + tt);
                        }
                        cond.add("JsonUtil.is" + capitalize(tt) + "(JsonUtil.getProperty(JsonUtil.toObject(json), \"${propertyName}\"))");
                    });
                    body.a("if (JsonUtil.isObjectWithProperty(json, \"${propertyName}\") && (" + String.join(" || ", cond) + ")) {");
                }
                break;
                case IsJsonObjectWithPropertyValue: {
                    var propertyName = rule.getName();
                    requireNonNull(propertyName);
                    body.c("propertyName", propertyName);
                    var type = rule.getJsonType();
                    requireNonNull(type);
                    body.c("type", capitalize(type));
                    var value = rule.getJsonValue();
                    requireNonNull(value);
                    body.a("if (JsonUtil.isObjectWithProperty(json, \"${propertyName}\") && " +
                            "JsonUtil.is${type}(JsonUtil.getProperty(JsonUtil.toObject(json), \"${propertyName}\")) && " +
                            "JsonUtil.equals(JsonUtil.getProperty(JsonUtil.toObject(json), \"${propertyName}\"), JsonUtil.parse${type}(" + escapeJavaString(value) + "))) {");
                }
                break;
                default: {
                    fail("Unknown union rule type: %s", rule.getRuleType());
                }
            }

            body.c("valueType", jnt.toJavaTypeString(false));
            body.c("readMethodName", readMethodName(jnt.getName(false, false)));

            if (nt.isPrimitiveType()) {
                assertion(!rootReachable.contains(t) || !rootReachable.contains(nt));
                body.c("toMethod", determineToVariant((PrimitiveType) nt, "json"));
                body.c("unionValueTypeImpl", jnt.getClassSource().getName());

                body.a("    ${valueType} value = ${toMethod};");
                body.a("    return new ${unionValueTypeImpl}(value);");

                readerClassSource.addImport(jnt.getClassSource());

            } else if (nt.isCollectionType()) {
                assertion(!rootReachable.contains(t) || !rootReachable.contains(nt));
                body.c("unionValueTypeImpl", jnt.getClassSource().getName());

                body.a("    ${valueType} value = ${readMethodName}(json);");
                body.a("    return new ${unionValueTypeImpl}(value);");

                readerClassSource.addImport(jnt.getClassSource());

            } else {
                if (rootReachable.contains(t) && rootReachable.contains(nt)) {
                    body.a("    return ${readMethodName}(json, isRoot);");
                } else {
                    body.a("    return ${readMethodName}(json);");
                }
            }

            body.a("} else ");
        });

        body.a("{");
        body.a("    return null;");
        body.a("}");

        methodSource.setBody(body.toString());
    }

    private void readMethodForListType(JavaClassSource readerClassSource, ListType t) {

        var jt = getState().getJavaIndex().requireType(t);
        jt.addImportsTo(readerClassSource);
        var vt = t.getValueType();
        var jvt = getState().getJavaIndex().requireType(vt);
        jvt.addImportsTo(readerClassSource);

        var methodSource = readerClassSource.addMethod()
                .setName(readMethodName(jt.getName(false, false)))
                .setReturnType(jt.toJavaTypeString(false))
                .setPublic();

        methodSource.addParameter(JsonNode.class.getSimpleName(), "json");

        var body = new BodyBuilder();

        body.c("valueType", jvt.toJavaTypeString(false));

        // PREFIX

        body.a("if (!JsonUtil.isArray(json)) {");
        body.a("    return null;");
        body.a("}");
        body.a("List<JsonNode> anyList = JsonUtil.toList(json);");
        body.a("if (anyList != null) {");
        body.a("    List<${valueType}> res = new ArrayList<>(anyList.size());");
        body.a("    for(JsonNode any: anyList) {");

        readerClassSource.addImport(ArrayList.class);

        if (vt.isPrimitiveType()) {

            body.c("toMethod", determineToVariant((PrimitiveType) vt, "any"));
            body.c("valueType", jvt.toJavaTypeString(false));

            body.a("        ${valueType} value = ${toMethod};");

        } else {

            body.c("readMethodName", readMethodName(jvt.getName(false, false)));
            body.a("        ${valueType} value = ${readMethodName}(any);");

        }
        body.append("        if(value != null) {");
        body.append("            res.add(value);");
        body.append("        } else {");
        body.append("            return null;");
        body.append("        }");

        // POSTFIX

        body.a("    }");
        body.a("    return res;");
        body.a("} else {");
        body.a("    return null;");
        body.a("}");

        methodSource.setBody(body.toString());
    }

    private void readMethodForMapType(JavaClassSource readerClassSource, MapType t) {

        var jt = getState().getJavaIndex().requireType(t);
        jt.addImportsTo(readerClassSource);
        var vt = t.getValueType();
        var jvt = getState().getJavaIndex().requireType(vt);
        jvt.addImportsTo(readerClassSource);

        var methodSource = readerClassSource.addMethod()
                .setName(readMethodName(jt.getName(false, false)))
                .setReturnType(jt.toJavaTypeString(false))
                .setPublic();

        methodSource.addParameter(JsonNode.class.getSimpleName(), "json");

        var body = new BodyBuilder();

        body.c("valueType", jvt.toJavaTypeString(false));

        // PREFIX

        body.a("if (!JsonUtil.isObject(json)) {");
        body.a("    return null;");
        body.a("}");
        body.a("Map<String, JsonNode> anyMap = JsonUtil.toMap(json);");
        body.a("if (anyMap != null) {");
        body.a("    Map<String, ${valueType}> res = new LinkedHashMap<>(anyMap.size());");
        body.a("    for(Entry<String, JsonNode> any: anyMap.entrySet()) {");

        readerClassSource.addImport(LinkedHashMap.class);
        readerClassSource.addImport(Entry.class);

        if (vt.isPrimitiveType()) {

            body.c("toMethod", determineToVariant((PrimitiveType) vt, "any.getValue()"));
            body.c("valueType", jvt.toJavaTypeString(false));

            body.a("        ${valueType} value = ${toMethod};");

        } else {

            body.c("readMethodName", readMethodName(jvt.getName(false, false)));
            body.a("        ${valueType} value = ${readMethodName}(any.getValue());");

        }

        body.append("        if(value != null) {");
        body.append("            res.put(any.getKey(), value);");
        body.append("        } else {");
        body.append("            return null;");
        body.append("        }");

        // POSTFIX

        body.a("    }");
        body.a("    return res;");
        body.a("} else {");
        body.a("    return null;");
        body.a("}");

        methodSource.setBody(body.toString());
    }


    /**
     * Creates code that will read any extra/remaining properties on a JSON object.
     *
     * @param body
     */
    private void createReadExtraPropertiesCode(BodyBuilder body) {
        body.append("ReaderUtil.readExtraProperties(json, node);");
    }


    private static String determineToVariant(PrimitiveType type, String var) {
        Class<?> _class = type.get_class();
        if (ObjectNode.class.equals(_class)) {
            return "JsonUtil.toObject(" + var + ")";
        } else if (JsonNode.class.equals(_class)) {
            return var;
        } else {
            return "JsonUtil.to" + _class.getSimpleName() + "(" + var + ")";
        }
    }


    private static String encodeRegex(String regex) {
        return regex.replace("\\", "\\\\");
    }
}
