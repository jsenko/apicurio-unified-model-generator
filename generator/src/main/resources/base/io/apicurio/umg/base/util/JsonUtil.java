package io.apicurio.datamodels.models.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonUtil {

    private static final JsonNodeFactory factory = JsonNodeFactory.instance;
    private static final ObjectMapper mapper = new ObjectMapper();

    // ========== Reading

    public static List<String> keys(ObjectNode json) {
        List<String> rval = new ArrayList<>();
        if (json != null) {
            Iterator<String> fieldNames = json.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                rval.add(fieldName);
            }
        }
        return rval;
    }

    public static List<String> matchingKeys(String regex, ObjectNode json) {
        return keys(json).stream().filter(key -> Pattern.matches(regex, key)).collect(Collectors.toList());
    }

    public static JsonNode getProperty(ObjectNode json, String propertyName) {
        if (json.has(propertyName)) {
            return json.get(propertyName);
        }
        return null;
    }

    public static void setProperty(ObjectNode json, String propertyName, JsonNode value) {
        if (value != null) {
            json.set(propertyName, value);
        }
    }

    /* Get/Consume a JSON (Any) property. */
    public static JsonNode getAnyProperty(ObjectNode json, String propertyName) {
        if (json.has(propertyName)) {
            JsonNode jsonNode = json.get(propertyName);
            if (!jsonNode.isNull()) {
                return jsonNode;
            }
        }
        return null;
    }

    public static JsonNode consumeAnyProperty(ObjectNode json, String propertyName) {
        if (json.has(propertyName)) {
            JsonNode rval = getAnyProperty(json, propertyName);
            json.remove(propertyName);
            return rval;
        }
        return null;
    }

    // ========== Parse

    public static String stringify(JsonNode json) {
        try {
            PrettyPrinter pp = new PrettyPrinter();
            return mapper.writer(pp).writeValueAsString(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseJSON(String jsonString) {
        try {
            return mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseBoolean(String partialJsonString) {
        Objects.requireNonNull(partialJsonString);
        if (StringUtils.equalsIgnoreCase(partialJsonString, "true")) {
            return BooleanNode.getTrue();
        } else if (StringUtils.equalsIgnoreCase(partialJsonString, "false")) {
            return BooleanNode.getFalse();
        } else {
            throw new RuntimeException(partialJsonString + " is not a boolean value");
        }
    }

    public static JsonNode parseString(String partialJsonString) {
        Objects.requireNonNull(partialJsonString);
        return TextNode.valueOf(partialJsonString);
    }

    public static JsonNode parseNumber(String partialJsonString) {
        Objects.requireNonNull(partialJsonString);
        try {
            var _long = Long.parseLong(partialJsonString);
            if (_long <= Integer.MAX_VALUE) {
                return IntNode.valueOf(Integer.parseInt(partialJsonString));
            }
            return LongNode.valueOf(_long);
        } catch (NumberFormatException ex1) {
            try {
                var _double = Double.parseDouble(partialJsonString);
                return DoubleNode.valueOf(_double);
            } catch (NumberFormatException ex2) {
                throw new RuntimeException(partialJsonString + " is not a number");
            }
        }
    }

    public static JsonNode parseObject(String partialJsonString) {
        Objects.requireNonNull(partialJsonString);
        return parseJSON(partialJsonString);
    }

    public static JsonNode parseArray(String partialJsonString) {
        Objects.requireNonNull(partialJsonString);
        var wrapper = parseJSON("{\"inner\":" + partialJsonString + "}"); // TODO hack
        var inner = wrapper.required("inner");
        if (!inner.isArray()) {
            throw new RuntimeException(partialJsonString + " is not an array");
        }
        return inner;
    }

    // ========== Clone

    public static JsonNode clone(JsonNode json) {
        try {
            TokenBuffer tb = new TokenBuffer(mapper, false);
            mapper.writeTree(tb, json);
            return mapper.readTree(tb.asParser());
        } catch (IOException e) {
            throw new RuntimeException("Error cloning JSON node.", e);
        }
    }

    public static <T> List<T> cloneAsList(Collection<T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    // ========== Create node

    public static ObjectNode objectNode() {
        return factory.objectNode();
    }

    public static ArrayNode arrayNode() {
        return factory.arrayNode();
    }

    public static TextNode textNode(String value) {
        return factory.textNode(value);
    }

    public static NumericNode numericNode(Number value) {
        if (value instanceof Byte) {
            return factory.numberNode(value.byteValue());
        } else if (value instanceof Short) {
            return factory.numberNode(value.shortValue());
        } else if (value instanceof Integer) {
            return factory.numberNode(value.intValue());
        } else if (value instanceof Long) {
            return factory.numberNode(value.longValue());
        } else if (value instanceof Float) {
            return factory.numberNode(value.floatValue());
        } else if (value instanceof Double) {
            return factory.numberNode(value.doubleValue());
        } else {
            return null;
        }
    }

    public static BooleanNode booleanNode(Boolean value) {
        return factory.booleanNode(value);
    }

    // ========== Write

    public static void arrayAdd(ArrayNode json, JsonNode value) {
        if (json != null && value != null) {
            json.add(value);
        }
    }

    public static void objectPut(ObjectNode json, String key, JsonNode value) {
        if (json != null && key != null && value != null) {
            json.put(key, value);
        }
    }

    // ========== Union test methods

    public static boolean equals(JsonNode left, JsonNode right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        return left.equals(right);
    }

    public static boolean isObjectWithProperty(JsonNode value, String propertyName) {
        if (value == null) {
            return false;
        }
        if (value.isObject()) {
            ObjectNode object = (ObjectNode) value;
            return object.has(propertyName);
        }
        return false;
    }

    public static boolean isObjectWithPropertyValue(JsonNode value, String propertyName, String propertyValue) {
        if (value == null) {
            return false;
        }
        if (value.isObject()) {
            ObjectNode object = (ObjectNode) value;
            if (object.has(propertyName)) {
                JsonNode pvalue = object.get(propertyName);
                if (!pvalue.isNull() && pvalue.isTextual()) {
                    String val = pvalue.asText();
                    return propertyValue.equals(val);
                }
            }
        }
        return false;
    }

    // ========== Is primitive

    public static boolean isString(JsonNode value) {
        return value != null && value.isTextual();
    }

    public static boolean isBoolean(JsonNode value) {
        return value != null && value.isBoolean();
    }

    public static boolean isInteger(JsonNode value) {
        return value != null && value.isIntegralNumber();
    }

    public static boolean isNumber(JsonNode value) {
        return value != null && value.isNumber();
    }

    // ========== Convert to primitive

    public static String toString(JsonNode value) {
        if (isString(value)) {
            return value.asText();
        } else {
            return null;
        }
    }

    public static Integer toInteger(JsonNode value) {
        if (isInteger(value)) {
            return value.asInt();
        } else {
            return null;
        }
    }

    public static Boolean toBoolean(JsonNode value) {
        if (isBoolean(value)) {
            return value.booleanValue();
        } else {
            return null;
        }
    }

    public static Number toNumber(JsonNode value) {
        if (isNumber(value)) {
            return value.numberValue();
        } else {
            return null;
        }
    }

    // ========== Is complex

    public static boolean isArray(JsonNode value) {
        return value != null && value.isArray();
    }

    public static boolean isObject(JsonNode value) {
        return value != null && value.isObject();
    }

    // ========== Convert to complex

    public static ObjectNode toObject(JsonNode value) {
        if (isObject(value)) {
            return (ObjectNode) value;
        } else {
            return null;
        }
    }

    public static ArrayNode toArray(JsonNode value) {
        if (isArray(value)) {
            return (ArrayNode) value;
        } else {
            return null;
        }
    }

    public static List<JsonNode> toList(JsonNode value) {
        if (!isArray(value)) {
            return null;
        }
        ArrayNode array = (ArrayNode) value;
        List<JsonNode> rval = new ArrayList<>(array.size());
        for (int idx = 0; idx < array.size(); idx++) {
            JsonNode node = array.get(idx);
            rval.add(node);
        }
        return rval;
    }

    public static Map<String, JsonNode> toMap(JsonNode value) {
        if (!isObject(value)) {
            return null;
        }
        ObjectNode object = (ObjectNode) value;
        Map<String, JsonNode> rval = new LinkedHashMap<>(object.size());
        keys(object).forEach(key -> {
            rval.put(key, object.get(key));
        });
        return rval;
    }

    // ==========

    private static class PrettyPrinter extends MinimalPrettyPrinter {
        private static final long serialVersionUID = -4446121026177697380L;

        private int indentLevel = 0;

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeStartObject(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void writeStartObject(JsonGenerator g) throws IOException {
            super.writeStartObject(g);
            indentLevel++;
            g.writeRaw("\n");
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeEndObject(com.fasterxml.jackson.core.JsonGenerator,
         * int)
         */
        @Override
        public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
            indentLevel--;
            g.writeRaw("\n");
            writeIndent(g);
            super.writeEndObject(g, nrOfEntries);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeStartArray(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void writeStartArray(JsonGenerator g) throws IOException {
            super.writeStartArray(g);
            indentLevel++;
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeEndArray(com.fasterxml.jackson.core.JsonGenerator,
         * int)
         */
        @Override
        public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
            g.writeRaw("\n");
            indentLevel--;
            writeIndent(g);
            super.writeEndArray(g, nrOfValues);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#beforeObjectEntries(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void beforeObjectEntries(JsonGenerator g) throws IOException {
            writeIndent(g);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#beforeArrayValues(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void beforeArrayValues(JsonGenerator g) throws IOException {
            g.writeRaw("\n");
            writeIndent(g);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeArrayValueSeparator(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
            super.writeArrayValueSeparator(g);
            g.writeRaw("\n");
            writeIndent(g);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeObjectEntrySeparator(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
            super.writeObjectEntrySeparator(g);
            g.writeRaw("\n");
            writeIndent(g);
        }

        /**
         * @see com.fasterxml.jackson.core.util.MinimalPrettyPrinter#writeObjectFieldValueSeparator(com.fasterxml.jackson.core.JsonGenerator)
         */
        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
            super.writeObjectFieldValueSeparator(g);
            g.writeRaw(" ");
        }

        private void writeIndent(JsonGenerator g) throws IOException {
            for (int idx = 0; idx < this.indentLevel; idx++) {
                g.writeRaw("    ");
            }
        }
    }

}
