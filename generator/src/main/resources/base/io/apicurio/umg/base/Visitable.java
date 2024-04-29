package io.apicurio.datamodels.models;

import io.apicurio.datamodels.models.visitors.Visitor;

public interface Visitable {

	void accept(Visitor visitor);
}
