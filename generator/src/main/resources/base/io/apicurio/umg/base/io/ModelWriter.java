package io.apicurio.datamodels.models.io;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.RootCapable;

public interface ModelWriter {

    ObjectNode writeRoot(RootCapable node);
}
