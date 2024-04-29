package io.apicurio.datamodels.models.union;

import io.apicurio.datamodels.models.Any;

public interface Union extends Any {

	Object getUnionValue();
}
