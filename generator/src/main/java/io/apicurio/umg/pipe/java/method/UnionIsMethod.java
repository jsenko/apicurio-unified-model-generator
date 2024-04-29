package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.model.source.MethodHolderSource;

import static io.apicurio.umg.logging.Errors.assertion;

public class UnionIsMethod {


    public static void create(GeneratorState state, IJavaType origin, MethodHolderSource<?> source, boolean body, IJavaType target, boolean active) {

        assertion(target != null);

//        var oldestParent = target;
//        while (oldestParent.getTypeModel().getParent() != null) {
//            oldestParent = state.getJavaIndex().requireType(oldestParent.getTypeModel().getParent());
//        }

        var method = source.addMethod()
                .setName(methodName(target))
                .setReturnType(boolean.class);

        if (origin.getTypeModel().getParent() != null) {
            method.addAnnotation(Override.class);
        }

        if (body) {
            if (active) {
                method.setBody("return true;");
            } else {
                method.setBody("return false;");
            }
            method.addAnnotation(Override.class);
            method.setPublic();
        }
    }

    public static String methodName(IJavaType target) {
        return "is" + target.getName(false, false);
    }
}
