package io.apicurio.datamodels.models.visitors;

import io.apicurio.datamodels.models.Any;
import io.apicurio.datamodels.models.MappedNode;
import io.apicurio.datamodels.models.Node;
import io.apicurio.datamodels.models.union.ListUnionValue;
import io.apicurio.datamodels.models.union.MapUnionValue;
import io.apicurio.datamodels.models.union.Union;
import io.apicurio.datamodels.models.util.JsonUtil;

import java.util.List;
import java.util.Map;

/**
 * Base class for all traversers.
 */
public abstract class AbstractTraverser implements Traverser, Visitor {

    protected final Visitor visitor;
    protected final TraversalContextImpl traversalContext = new TraversalContextImpl();

    public AbstractTraverser(Visitor visitor) {
        this.visitor = visitor;
        if (visitor instanceof TraversingVisitor) {
            ((TraversingVisitor) visitor).setTraversalContext(this.traversalContext);
        }
    }

    /**
     * Traverse the given node. Guaranteed to not be null here.
     */
    protected void doTraverseNode(Node node) {
        node.accept(this);
    }

    /**
     * Traverse into the given node, unless it's null.
     */
    protected void traverseNode(String propertyName, Node node) {
        if (node != null) {
            traversalContext.pushProperty(propertyName);
            doTraverseNode(node);
            traversalContext.pop();
        }
    }

    /**
     * Traverse the items of the given array.
     */
    protected void traverseAnyList(String propertyName, List<? extends Any> list) {
        if (list != null) {
            int index = 0;
            traversalContext.pushProperty(propertyName);
            @SuppressWarnings("unchecked")
            List<Any> clonedList = JsonUtil.cloneAsList((List<Any>) list);
            for (Any node : clonedList) {
                if (node != null && node.isNode()) {
                    traversalContext.pushListIndex(index);
                    doTraverseNode((Node) node);
                    traversalContext.pop();
                }
                index++;
            }
            traversalContext.pop();
        }
    }

    /**
     * Traverse the items of the given map.
     */
    protected void traverseAnyMap(String propertyName, Map<String, ? extends Any> map) {
        if (map != null) {
            traversalContext.pushProperty(propertyName);
            List<String> keys = JsonUtil.cloneAsList(map.keySet());
            keys.forEach(key -> {
                Any value = map.get(key);
                if (value != null && value.isNode()) {
                    traversalContext.pushMapIndex(key);
                    doTraverseNode((Node) value);
                    traversalContext.pop();
                }
            });
            this.traversalContext.pop();
        }
    }

    /**
     * Traverse the items of the given mapped node.
     */
    protected void traverseMappedNode(MappedNode<Any> mappedNode) { // TODO How to distinguish from primitives?
        if (mappedNode != null) {
            List<String> names = JsonUtil.cloneAsList(mappedNode.getItemNames());
            names.forEach(name -> {
                Any item = mappedNode.getItem(name);
                if (item != null && item.isNode()) {
                    traversalContext.pushMapIndex(name);
                    doTraverseNode((Node) item);
                    traversalContext.pop();
                }
            });
        }
    }

    /**
     * Traverse a union property. Traversal of a union property only needs to happen
     * if the value of the union is an entity or an entity collection.
     */
    protected void traverseUnion(String propertyName, Union union) {
        if (union != null) {
            if (union.isNode()) {
                traverseNode(propertyName, (Node) union);
            } else if (union.isListUnionValueWithAny()) {
                @SuppressWarnings("unchecked")
                List<Any> values = ((ListUnionValue<Any>) union).getUnionValue();
                traverseAnyList(propertyName, values);
            } else if (union.isMapUnionValueWithAny()) {
                @SuppressWarnings("unchecked")
                Map<String, Any> values = ((MapUnionValue<Any>) union).getUnionValue();
                traverseAnyMap(propertyName, values);
            }
        }
    }

    /**
     * Called to traverse the data model starting at the given node and traversing
     * down until this node and all child nodes have been visited.
     */
    @Override
    public void traverse(Node node) {
        node.accept(this);
    }
}
