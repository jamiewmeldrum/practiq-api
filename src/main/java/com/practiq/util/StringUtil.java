package com.practiq.util;

// Null-safe blank check. The JDK's String.isBlank() is an instance method, so it cannot answer for a
// null, and Micronaut's StringUtils only offers isEmpty — hence three lines here rather than a
// dependency on a cloud SDK's utilities to ask a question about a string.
public class StringUtil {

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
