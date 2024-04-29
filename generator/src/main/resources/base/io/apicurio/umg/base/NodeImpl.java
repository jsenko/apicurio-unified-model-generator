package io.apicurio.datamodels.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public abstract class NodeImpl implements Node {

    private static int __modelIdCounter = 0;

    private final int _modelId = __modelIdCounter++;
    private Node _parent;
    private Map<String, JsonNode> _extraProperties;
    private Map<String, Object> _attributes;

    @Override
    public RootCapable root() {
        if (!this.isAttached()) {
            throw new IllegalStateException("Node is not attached.");
        }
        return this.parent().root();
    }

    @Override
    public Node parent() {
        return this._parent;
    }

    @Override
    public int modelId() {
        return this._modelId;
    }

    @Override
    public Object getNodeAttribute(String attributeName) {
        if (this._attributes != null) {
            return this._attributes.get(attributeName);
        } else {
            return null;
        }
    }

    @Override
    public void setNodeAttribute(String attributeName, Object attributeValue) {
        if (this._attributes == null) {
            this._attributes = new HashMap<>();
        }
        if (attributeValue != null) {
            this._attributes.put(attributeName, attributeValue);
        } else {
            this._attributes.remove(attributeName);
        }
    }

    @Override
    public Set<String> getNodeAttributeNames() {
        if (this._attributes != null) {
            return Collections.unmodifiableSet(this._attributes.keySet());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void clearNodeAttributes() {
        if (this._attributes != null) {
            this._attributes.clear();
        }
    }

    @Override
    public void addExtraProperty(String key, JsonNode value) {
        if (this._extraProperties == null) {
            this._extraProperties = new LinkedHashMap<>();
        }
        this._extraProperties.put(key, value);
    }

    @Override
    public JsonNode removeExtraProperty(String name) {
        if (this._extraProperties != null) {
            return this._extraProperties.remove(name);
        }
        return null;
    }

    @Override
    public boolean hasExtraProperties() {
        return this._extraProperties != null && this._extraProperties.size() > 0;
    }

    @Override
    public List<String> getExtraPropertyNames() {
        if (this.hasExtraProperties()) {
            return List.copyOf(this._extraProperties.keySet());
        }
        return Collections.emptyList();
    }

    @Override
    public JsonNode getExtraProperty(String name) {
        if (this.hasExtraProperties()) {
            return this._extraProperties.get(name);
        }
        return null;
    }

    @Override
    public boolean isAttached() {
        return this.parent() != null;
    }

    @Override
    public void attachTo(Node parent) {
        if (!parent.isAttached()) {
            throw new IllegalArgumentException("Target parent node is not itself attached.");
        }
        this._parent = parent;
    }

    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    public boolean isListUnionValueWithAny() {
        return false;
    }

    @Override
    public boolean isMapUnionValueWithAny() {
        return false;
    }
}
