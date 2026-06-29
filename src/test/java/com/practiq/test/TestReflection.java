package com.practiq.test;

import java.lang.reflect.Field;

// Test-only: sets otherwise-unsettable fields (e.g. DB-assigned, setter-less) by reflection.
public final class TestReflection {

    private TestReflection() {
    }

    public static <T> T setField(T target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("test setup: cannot set field '" + fieldName + "'", e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // walk up to the superclass
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
