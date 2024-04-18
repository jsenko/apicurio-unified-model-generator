package io.apicurio.umg.pipe.java.method;

import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

public class UnionAsMethod {

    private void addInterfaceMethod(JavaInterfaceSource _interface) {
        String isMethodName = "is" + typeName;
        _interface.addMethod().setName(isMethodName).setReturnType(boolean.class).setPublic();
    }
}
