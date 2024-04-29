package io.apicurio.umg.logging;

import static io.apicurio.umg.logging.Logger.error;

public class Errors {

    public static void fail(String message, Object... args) {
        error(message, args);
        throw new RuntimeException(String.format(message, args));
    }

    public static void assertion(boolean expression, String message, Object... args) {
        if (!expression) {
            fail("Assertion failed: " + message, args);
        }
    }

    public static void assertion(boolean expression) {
        assertion(expression, "Assertion failed");
    }
}
