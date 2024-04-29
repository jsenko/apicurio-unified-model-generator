package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.jboss.forge.roaster.model.Named;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.MethodHolderSource;

import static io.apicurio.umg.logging.Errors.assertion;

public class UnionAsMethod {


    public static void create(GeneratorState state, IJavaType origin, MethodHolderSource<?> source, boolean body, IJavaType target, boolean active) {

        assertion(target != null);

        var oldestParent = target;
        while (oldestParent.getTypeModel().getParent() != null) {
            oldestParent = state.getJavaIndex().requireType(oldestParent.getTypeModel().getParent());
        }

        var method = source.addMethod()
                .setName(methodName(oldestParent))
                .setReturnType(target.getTypeModel().isLeaf() ? target.toJavaTypeString(false) : target.toJavaTypeStringWithExtends());

        if (origin.getTypeModel().getParent() != null) {
            method.addAnnotation(Override.class);
        }

        if (body) {
            if (active) {
                if (((Named) source).getName().contains(target.getName(true, false) + "UnionValue")) { // TODO Hack
                    method.setBody("return getUnionValue();");
                } else {
                    method.setBody("return this;");
                }
            } else {
                method.setBody("throw new ClassCastException();");
            }
            method.addAnnotation(Override.class);
            method.setPublic();
        }

        target.addImportsTo((Importer<?>) source);
    }

    public static String methodName(IJavaType target) {
        return "as" + target.getName(false, false);
    }
}
