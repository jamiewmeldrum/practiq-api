package utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// Test-only: sets otherwise-unsettable fields (e.g. DB-assigned, setter-less) by reflection.
public final class TestReflection {

    private TestReflection() {
    }

    // Fails if any declared instance field on target is null. Used as an exhaustiveness tripwire:
    // an "all fields populated" expectation stays honest when new fields are added to the class,
    // because a newly added field left unset trips this before the value assertions run.
    //
    // Rejects primitive fields outright: a primitive can't be distinguished from "unset" (an omitted
    // request param leaves it at its JVM default, indistinguishable from a real default value), so
    // this guard could never judge one. Request DTO fields must be nullable wrappers.
    public static void assertAllFieldsSet(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType().isPrimitive()) {
                throw new AssertionError("Field '" + field.getName() + "' on "
                        + target.getClass().getSimpleName() + " is primitive — use a nullable wrapper"
                        + " so 'not set' is distinguishable from its default value.");
            }
            field.setAccessible(true);
            try {
                if (field.get(target) == null) {
                    throw new AssertionError("Field '" + field.getName() + "' on "
                            + target.getClass().getSimpleName() + " is not set (null).");
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("test setup: cannot read field '" + field.getName() + "'", e);
            }
        }
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
