package io.apicurio.datamodels.models.union;

public interface UnionValue<T> extends Union {

	void setUnionValue(T value);

	@Override
	T getUnionValue();
}
