package io.apicurio.umg.pipe;

import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.type.ListTypeModel;
import io.apicurio.umg.models.concept.type.MapTypeModel;
import lombok.Getter;
import org.modeshape.common.text.Inflector;

/**
 * Base class for all pipeline stages.
 */
public abstract class AbstractStage implements Stage {

    private static final Inflector inflector = new Inflector();

    @Getter
    private GeneratorState state;

    @Override
    public final void process(GeneratorState state) {
        this.state = state;
        debug("Executing stage.");
        this.doProcess();
    }

    /**
     * Perform the logic of the stage.
     */
    protected abstract void doProcess();

    protected boolean isStarProperty(PropertyModel property) {
        return "*".equals(property.getName());
    }

    protected boolean isRegexProperty(PropertyModel property) {
        return property.getName().startsWith("/");
    }

    protected boolean isEntityList(PropertyModel property) {
        return property.getType().isListType() && ((ListTypeModel)property.getType()).getValueType().isEntityType();
    }

    protected boolean isEntityMap(PropertyModel property) {
        return property.getType().isMapType() && ((MapTypeModel)property.getType()).getValueType().isEntityType();
    }

    protected boolean isEntity(PropertyModel property) {
        return property.getType().isEntityType();
    }

    protected boolean isUnion(PropertyModel property) {
        return property.getType().isUnionType();
    }

    protected boolean isPrimitive(PropertyModel property) {
        return property.getType().isPrimitiveType();
    }

    protected boolean isPrimitiveList(PropertyModel property) {
        return property.getType().isListType() && ((ListTypeModel)property.getType()).getValueType().isPrimitiveType();
    }

    protected boolean isPrimitiveMap(PropertyModel property) {
        return property.getType().isMapType() && ((MapTypeModel)property.getType()).getValueType().isPrimitiveType();
    }

    protected String singularize(String name) {
        return inflector.singularize(name);
    }

    protected String extractRegex(String propertyName) {
        return propertyName.substring(1, propertyName.length() - 1);
    }

    protected void info(String message, Object... args) {
        Logger.info("[" + getClass().getSimpleName() + "] " + message, args);
    }

    protected void warn(String message, Object... args) {
        Logger.warn("[" + getClass().getSimpleName() + "] " + message, args);
    }

    protected void debug(String message, Object... args) {
        Logger.debug("[" + getClass().getSimpleName() + "] " + message, args);
    }

    protected void error(String message, Object... args) {
        Logger.error("[" + getClass().getSimpleName() + "] " + message, args);
    }

    protected void fail(String message, Object... args) {
        throw new RuntimeException(String.format(message, args));
    }

    protected void assertion(boolean expression) {
        assertion(expression, "Assertion failed");
    }

    protected void assertion(boolean expression, String message, Object... args) {
        if(!expression) {
            fail("Assertion failed: " + message, args);
        }
    }
}
