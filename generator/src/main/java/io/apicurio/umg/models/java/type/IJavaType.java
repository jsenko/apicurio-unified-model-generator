package io.apicurio.umg.models.java.type;

import io.apicurio.umg.models.concept.typelike.TypeLike;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import static io.apicurio.umg.logging.Errors.fail;

public interface IJavaType {

    /**
     * This method is general and the meaning of the return value is specific to the underlying type.
     * For example, for an entity type it represents entity interface,
     * but for a collection type it represents a union value interface.
     * Even if the type implements this method, it may return null.
     * It is still useful to have these methods despite the drawbacks,
     * to avoid instanceof and casts.
     * The caller is responsible for making sure the type is supported and valid for a given operation.
     */
    default JavaInterfaceSource getInterfaceSource() {
        fail("This Java type can't be represented by a Java interface.");
        return null; // Unreachable
    }

    default void setInterfaceSource(JavaInterfaceSource interfaceSource) {
        fail("This Java type can't be represented by a Java interface.");
    }

    default JavaClassSource getClassSource() {
        fail("This Java type can't be represented by a Java class.");
        return null; // Unreachable
    }

    default void setClassSource(JavaClassSource classSource) {
        fail("This Java type can't be represented by a Java class.");
    }

    // Get FQN
    default String getPackageName() {
        return getTypeModel().getNamespace();
    }

    /**
     * Get a name of the java type suitable for use as class name.
     */
    String getName(boolean withPrefix, boolean impl);


    void addImportsTo(Importer<?> importer);


    /**
     * Get a name of the java type suitable for use as method parameters or return types.
     * This is mostly used for complex types, such as list union values, which returns List<...>
     */
    String toJavaTypeString(boolean impl);

    /**
     * Same as toJavaTypeString, but in case of collections, returns
     * `Collection<? extends Foo>` instead of `Collection<Foo>`
     */
    default String toJavaTypeStringWithExtends() {
        return toJavaTypeString(false);
    }

    TypeLike getTypeModel();
}
