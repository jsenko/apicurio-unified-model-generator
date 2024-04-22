package io.apicurio.umg.pipe.java.type;

import io.apicurio.umg.models.concept.type.TypeModel;
import org.jboss.forge.roaster.model.source.Importer;

public interface IJavaType {

    // Get FQN
    String getPackageName();

    String getName();

    void addImportsTo(Importer<?> importer);

    String toJavaTypeString();

    TypeModel getTypeModel();
}
