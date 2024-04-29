package io.apicurio.datamodels.models;

import io.apicurio.datamodels.models.union.ListUnionValue;
import io.apicurio.datamodels.models.union.MapUnionValue;

import java.util.List;
import java.util.Map;

public interface Any {

	boolean isNode();

	boolean isListUnionValueWithAny();

	boolean isMapUnionValueWithAny();

	static void attach(Any value, Node parent) {
		if (value.isNode()) {
			((Node) value).attachTo(parent);
		} else if (value.isListUnionValueWithAny()) {
			@SuppressWarnings("unchecked")
			List<Any> values = ((ListUnionValue<Any>) value).getUnionValue();
			values.forEach(v -> attach(v, parent));
		} else if (value.isMapUnionValueWithAny()) {
			@SuppressWarnings("unchecked")
			Map<String, Any> values = ((MapUnionValue<Any>) value).getUnionValue();
			values.values().forEach(v -> attach(v, parent));
		}
	}
}
