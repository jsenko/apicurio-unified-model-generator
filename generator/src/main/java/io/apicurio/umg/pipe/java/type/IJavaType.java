package io.apicurio.umg.pipe.java.type;

import org.jboss.forge.roaster.model.source.Importer;

public interface IJavaType {

    // Get FQN

    void addImportsTo(Importer<?> importer);

    String toJavaTypeString();
}
