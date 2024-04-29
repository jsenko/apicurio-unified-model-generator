package io.apicurio.umg.models.java.type;

import io.apicurio.umg.index.java.JavaIndex;
import io.apicurio.umg.models.concept.type.CollectionType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public abstract class CollectionJavaType implements IJavaType {

    @Getter
    @EqualsAndHashCode.Include
    protected final CollectionType typeModel;

    protected final String prefix;
    protected final JavaIndex index;

    /**
     * When a list (this type) is used as a nested type in a union,
     * it has to be wrapped in a "union value type",
     * essentially because we cannot make a list extend a union interface.
     * If this is the case, the following contains the generated java interface source,
     * otherwise is null.
     */
    @Getter
    @Setter
    protected JavaInterfaceSource unionValueInterfaceSource;

    /**
     * Same as above, but implementation of the interface.
     */
    @Getter
    @Setter
    protected JavaClassSource unionValueClassSource;

    @Override
    public JavaInterfaceSource getInterfaceSource() {
        return unionValueInterfaceSource;
    }

    @Override
    public void setInterfaceSource(JavaInterfaceSource interfaceSource) {
        this.unionValueInterfaceSource = interfaceSource;
    }

    @Override
    public JavaClassSource getClassSource() {
        return unionValueClassSource;
    }

    @Override
    public void setClassSource(JavaClassSource classSource) {
        this.unionValueClassSource = classSource;
    }

    public CollectionJavaType(CollectionType typeModel, String prefix, JavaIndex index) {
        this.typeModel = typeModel;
        this.prefix = prefix;
        this.index = index;
    }

//    @Override
//    public String getPackageName() {
//        return typeModel.getNamespace();
//    }
}
