package io.apicurio.datamodels.models;

import java.util.List;

public interface MappedNode<T> extends Node {

    /**
     * Gets a child item by name.
     *
     * @return <code>null</code> if not found.
     */
    T getItem(String name);

    /**
     * @return an unmodifiable list of all child items, in order.
     */
    List<T> getItems();

    /**
     * @return an unmodifiable list of names of all child items, in order.
     */
    List<String> getItemNames();

    /**
     * Adds a child item under the given name.
     * Order of insertion is preserved.
     */
    void addItem(String name, T item);

    /**
     * Removes a child item by name.
     *
     * @return the removed item or <code>null</code> if not found.
     */
    T removeItem(String name);
}
