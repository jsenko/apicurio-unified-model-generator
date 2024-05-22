package io.apicurio.umg.pipe.java;

import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.models.concept.ConceptUtils;
import io.apicurio.umg.models.concept.VisitorModel;
import io.apicurio.umg.models.concept.type.EntityType;
import io.apicurio.umg.models.java.type.EntityJavaType;
import io.apicurio.umg.pipe.java.method.BodyBuilder;
import io.apicurio.umg.pipe.java.method.GetterMethod;
import io.apicurio.umg.pipe.java.method.JavaUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;

import java.util.*;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;

/**
 * Creates a traverser for each specification visitor interface.  A traverser is a visitor that
 * knows how to traverse the data model.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateTraversersStage extends AbstractVisitorStage {

    @Override
    protected void doProcess() {
        // Create a visitor adapter for each spec version visitor
        getState().getSpecIndex().getAllSpecificationVersions().forEach(specVer -> {
            createTraverser(specVer);
        });
    }

    private void createTraverser(SpecificationVersion specVer) {
        VisitorModel visitor = getState().getConceptIndex().lookupVisitor(specVer.getNamespace());

        String traverserPackageName = getTraverserPackageName(specVer);
        String traverserClassName = getTraverserClassName(specVer);

        debug("Creating traverser: " + traverserClassName);

        // Create the traverser class
        JavaClassSource traverserSource = Roaster.create(JavaClassSource.class)
                .setPackage(traverserPackageName)
                .setName(traverserClassName)
                .setPublic();

        // Extend the AbstractTraverser class
        JavaClassSource abstractTraverserSource = getState().getJavaIndex().lookupClass(getAbstractTraverserFQN());
        traverserSource.addImport(abstractTraverserSource);
        traverserSource.extendSuperType(abstractTraverserSource);

        // Create the constructor
        JavaInterfaceSource rootVisitorJavaInterface = getState().getJavaIndex().lookupInterface(getRootVisitorInterfaceFQN());
        MethodSource<JavaClassSource> constructor = traverserSource.addMethod().setConstructor(true).setPublic();
        constructor.addParameter(rootVisitorJavaInterface, "visitor");
        constructor.setBody("super(visitor);");

        // Determine which visitors this adapter is implementing (only one right now)
        Set<VisitorModel> visitorsToImplement = Collections.singleton(visitor);

        // For each visitor, lookup its Java interface, add it to the list of interfaces implemented,
        // and then collect all methods in the interface (avoiding potential duplicates).
        List<MethodSource<?>> methodsToImplement = new LinkedList<MethodSource<?>>();
        Set<String> methodNames = new HashSet<>();
        for (VisitorModel visitorToImplement : visitorsToImplement) {
            JavaInterfaceSource vtiInterface = lookupJavaVisitor(visitorToImplement);
            if (vtiInterface == null) {
                warn("Visitor interface not found: " + visitorToImplement);
            }

            traverserSource.addImport(vtiInterface);
            traverserSource.addInterface(vtiInterface);

            // Add all methods to the list (but avoid duplicates).
            List<MethodSource<?>> allMethods = getAllMethodsForVisitorInterface(visitorToImplement);
            allMethods.forEach(method -> {
                if (!methodNames.contains(method.getName())) {
                    methodsToImplement.add(method);
                    methodNames.add(method.getName());
                }
            });
        }

        // Now create a traversing implementation for each visit method.
        methodsToImplement.forEach(method -> {
            MethodSource<JavaClassSource> methodSource = traverserSource.addMethod()
                    .setName(method.getName())
                    .setReturnTypeVoid()
                    .setPublic();

            // We know each visit method will have a single parameter.
            ParameterSource<?> param = method.getParameters().get(0);
            traverserSource.addImport(param.getType());
            methodSource.addParameter(param.getType().getSimpleName(), param.getName());
            methodSource.addAnnotation(Override.class);

            String entityNamespace = specVer.getNamespace();
            String entityName = method.getName().replace("visit", "");

            //EntityModel entityModel = getState().getConceptIndex().lookupEntity(entityNamespace, entityName);
            var entityType = getState().getConceptIndex().getTypes().stream()
                    .filter(t -> t.isEntityType() && t.getNamespace().equals(entityNamespace) && t.getName().equals(entityName))
                    .findAny()
                    .orElse(null);

            assertion(entityType != null);

            String body = createTraversalMethodBody(traverserSource, (EntityType) entityType);
            methodSource.setBody(body);
        });

        // Index the new class
        getState().getJavaIndex().index(traverserSource);
    }

    private String createTraversalMethodBody(JavaClassSource traverserSource, EntityType type) {

        EntityJavaType jt = (EntityJavaType) getState().getJavaIndex().requireType(type);

        BodyBuilder body = new BodyBuilder();

        body.c("type", jt.getInterfaceSource().getName());
        traverserSource.addImport(jt.getInterfaceSource());

        body.a("${type} n = (${type}) node;");
        body.a("n.accept(this.visitor);");

        var allProperties = JavaUtils.extractProperties(jt, true);

        allProperties.forEach(p -> {
            var pt = p.getType();
            var jpt = getState().getJavaIndex().requireType(pt);

            body.c("propertyName", p.getName());
            body.c("propertyGetter", GetterMethod.methodName(p, jpt));

            var containsEntity = ConceptUtils.collectNestedTypes(pt).stream().anyMatch(t -> t.isEntityType());

            if (containsEntity) {
                if (pt.isEntityType()) {

                    if (p.isStar()) {
                        body.append("traverseMappedNode(n);");
                    } else if (p.isRegex()) {
                        body.append("traverseAnyMap(\"${propertyName}\", n.${propertyGetter}());");
                    } else {
                        body.append("traverseNode(\"${propertyName}\", n.${propertyGetter}());");
                    }

                } else if (pt.isListType()) {

                    body.append("traverseAnyList(\"${propertyName}\", n.${propertyGetter}());");

                } else if (pt.isMapType()) {

                    body.append("traverseAnyMap(\"${propertyName}\", n.${propertyGetter}());");

                } else if (pt.isUnionType()) {

                    body.append("traverseUnion(\"${propertyName}\", n.${propertyGetter}());");

                } else if (pt.isPrimitiveType()) {
                    // ignored
                } else {
                    fail("Unhandled property in traverser");
                }
            }
        });

        return body.toString();
    }

}
