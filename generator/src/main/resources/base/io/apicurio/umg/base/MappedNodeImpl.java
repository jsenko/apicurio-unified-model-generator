package io.apicurio.datamodels.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class MappedNodeImpl<T> extends NodeImpl implements MappedNode<T> {

    private Map<String, T> _items;

    @Override
    public T getItem(String name) {
        if (this._items != null) {
            return this._items.get(name);
        } else {
            return null;
        }
    }

    @Override
    public List<T> getItems() {
        if (this._items != null) {
            return List.copyOf(this._items.values());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getItemNames() {
        if (this._items != null) {
            return List.copyOf(this._items.keySet());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void addItem(String name, T item) {
        if (this._items == null) {
            this._items = new LinkedHashMap<>();
        }
        this._items.put(name, item);
    }

    @Override
    public T removeItem(String name) {
        if (this._items != null) {
            return this._items.remove(name);
        } else {
            return null;
        }
    }
}
