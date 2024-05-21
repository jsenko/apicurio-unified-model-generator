package io.apicurio.datamodels.models.union;

import java.util.ArrayList;
import java.util.List;

public abstract class ListUnionValueImpl<T> extends UnionValueImpl<List<T>> implements ListUnionValue<T> {

	public ListUnionValueImpl() {
		super(new ArrayList<>());
	}

	public ListUnionValueImpl(List<T> value) {
		super(value);
	}
}
