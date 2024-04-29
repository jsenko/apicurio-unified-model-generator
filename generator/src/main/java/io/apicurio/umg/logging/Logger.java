/*
 * Copyright 2020 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.umg.logging;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * @author eric.wittmann@gmail.com
 */
public class Logger {

    public static void info(String message, Object... args) {
        out.println("INFO:  " + String.format(message, args));
    }

    public static void debug(String message, Object... args) {
        out.println("DEBUG: " + String.format(message, args));
    }

    public static void warn(String message, Object... args) {
        err.println("WARN:  " + String.format(message, args));
    }

    public static void error(String message, Object... args) {
        err.println("ERROR: " + String.format(message, args));
    }

    public static void error(Throwable t) {
        error(t.getMessage());
        t.printStackTrace(err);
    }
}
