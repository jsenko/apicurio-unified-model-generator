package io.apicurio.umg.pipe.java.method;

import io.apicurio.umg.models.concept.type.UnionType;
import io.apicurio.umg.models.java.type.IJavaType;
import io.apicurio.umg.pipe.GeneratorState;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.MethodHolderSource;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.pipe.java.method.JavaUtils.markOverridden;

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
            markOverridden(method);
        }

        if (body) {
            if (active) {
                if (target.getTypeModel().isEntityType() ||
                        (origin.getTypeModel().isUnionType()
                                && target.getTypeModel().isUnionType()
                                && ((UnionType) origin.getTypeModel()).getTypes().contains(target.getTypeModel())
                        )
                ) {
                    method.setBody("return this;");
                } else {
                    method.setBody("return getUnionValue();");
                }
            } else {
                method.setBody("throw new ClassCastException();");
            }
            markOverridden(method);
            method.setPublic();
        }

        target.addImportsTo((Importer<?>) source);
    }

    public static String methodName(IJavaType target) {
        return "as" + target.getName(false, false);
    }
}
