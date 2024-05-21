package io.apicurio.datamodels.models.union;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class MapUnionValueImpl<T> extends UnionValueImpl<Map<String, T>> implements MapUnionValue<T> {

	public MapUnionValueImpl() {
		super(new LinkedHashMap<String, T>());
	}

	public MapUnionValueImpl(Map<String, T> value) {
		super(value);
	}
}
