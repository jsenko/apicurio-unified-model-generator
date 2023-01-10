package io.apicurio.umg.pipe;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static boolean nullableBoolean(Boolean value) {
        return value != null && value;
    }


    public static <T> List<T> copy(List<T> source) {
        return new ArrayList<>(source);
    }
}
