package io.apicurio.umg.pipe.java.method;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import io.apicurio.umg.logging.Logger;

public class BodyBuilder {

    private StringBuilder str = new StringBuilder();
    private Map<String, String> context = new HashMap<>();
    private int indent = 0;

    public static BodyBuilder create() {
        return new BodyBuilder();
    }

    public BodyBuilder indent() {
        indent++;
        return this;
    }

    public BodyBuilder deindent() {
        if(indent == 0) {
            throw new IllegalStateException("Indentation level is already zero.");
        }
        indent--;
        return this;
    }

    public BodyBuilder c(String name, String value) {
        addContext(name, value);
        return this;
    }

    public void addContext(String name, String value) {
        if (value == null) {
            Logger.warn("[BodyBuilder] Adding null value to BodyBuilder for: " + name);
        } else {
            context.put(name, value);
        }
    }

    public String getContext(String name) {
        var val = context.get(name);
        Objects.requireNonNull(val);
        return val;
    }

    public void clearContext() {
        context.clear();
    }

    public BodyBuilder a(String line) {
        append(line);
        return this;
    }

    public void append(String line) {
        String lineResolved = line;
        for (Entry<String, String> entry : context.entrySet()) {
            lineResolved = lineResolved.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        str.append("    ".repeat(indent));
        str.append(lineResolved);
        str.append("\n");

        if (lineResolved.contains("${")) {
            Logger.warn("[BodyBuilder] 'append' detected unresolved variables: " + lineResolved);
        }
    }

    @Override
    public String toString() {
        // TODO Perform string building lazily (on-demand)
        return str.toString();
    }

    public static String escapeJavaString(String value) {
        value = value.replace("\\", "\\\\");
        value = value.replace("\"", "\\\"");
        return "\"" + value + "\"";
    }
}
