package io.apicurio.datamodels.models.union;

import io.apicurio.datamodels.models.visitors.Visitor;

public abstract class UnionValueImpl<T> implements UnionValue<T> {

	private T value;

	public UnionValueImpl() {
	}

	public UnionValueImpl(T value) {
		this.value = value;
	}

	@Override
	public T getUnionValue() {
		return value;
	}

	@Override
	public void setUnionValue(T value) {
		this.value = value;
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isListUnionValueWithAny() {
		return false;
	}

	@Override
	public boolean isMapUnionValueWithAny() {
		return false;
	}

	@Override
	public void accept(Visitor visitor) {
	}

}
