package io.apicurio.umg.base.io;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.apicurio.datamodels.models.RootCapable;

public interface ModelReader {

    RootCapable readRoot(ObjectNode json);
}
