package io.apicurio.datamodels.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

public interface Node extends Any, Visitable {

    int modelId();

    RootCapable root();

    Node parent();

    boolean isAttached();

    void attachTo(Node parent);

    /**
     * Gets a node attribute by name.
     *
     * @return <code>null</code> if not found.
     */
    Object getNodeAttribute(String attributeName);

    /**
     * Sets an attribute under the given name.
     * Order of insertion is NOT preserved.
     * <code>null</code> values are NOT allowed, and will cause any existing attribute with the given name to be removed.
     */
    void setNodeAttribute(String attributeName, Object attributeValue);

    /**
     * @return an unmodifiable set of names of all attibutes.
     */
    Set<String> getNodeAttributeNames();

    /**
     * Removes all node attributes.
     */
    void clearNodeAttributes();

    /**
     * Adds an extra property under the given name.
     * Order of insertion is preserved.
     * <code>null</code> names and values are allowed.
     */
    void addExtraProperty(String key, JsonNode value);

    /**
     * Removes an extra property by name.
     *
     * @return the removed item or <code>null</code> if not found. Note that <code>null</code> names and values are allowed.
     */
    JsonNode removeExtraProperty(String name);

    boolean hasExtraProperties();

    /**
     * @return an unmodifiable list of names of all extra properties, in order.
     */
    List<String> getExtraPropertyNames();

    /**
     * Gets an extra property by name.
     *
     * @return the removed item or <code>null</code> if not found. Note that <code>null</code> names and values are allowed.
     */
    JsonNode getExtraProperty(String name);

    Node emptyClone();
}
