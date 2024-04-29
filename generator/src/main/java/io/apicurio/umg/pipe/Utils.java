package io.apicurio.umg.pipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {

    public static boolean nullableBoolean(Boolean value) {
        return value != null && value;
    }

    public static <T> List<T> copy(List<T> source) {
        return new ArrayList<>(source);
    }

    public static <T> void addIfNotNull(Collection<T> collection, T value) {
        if (value != null) {
            collection.add(value);
        }
    }
}
