package aporia.su.util.user.render.web;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

final class ReflectionSupport {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ReflectionSupport() {
    }

    static Optional<Class<?>> findFirstClass(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Optional.of(Class.forName(candidate));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return Optional.empty();
    }

    static Optional<Method> findMethod(Class<?> clazz, String... candidates) {
        for (String name : candidates) {
            try {
                Method method = clazz.getMethod(name);
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return Optional.empty();
    }

    static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return Optional.of(method);
        } catch (NoSuchMethodException ex) {
            return Optional.empty();
        }
    }

    static Optional<Method> findMethodByParams(Class<?> clazz, String name, int minParams, int maxParams) {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            int count = method.getParameterCount();
            if (count >= minParams && count <= maxParams) {
                method.setAccessible(true);
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to invoke " + method, ex);
        }
    }

    static MethodHandle findHandle(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return LOOKUP.findVirtual(clazz, name, MethodType.methodType(void.class, parameterTypes));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    static double coerceNumber(Class<?> targetType, double value) {
        if (targetType == double.class || targetType == Double.class) return value;
        if (targetType == float.class || targetType == Float.class) return (float) value;
        if (targetType == int.class || targetType == Integer.class) return (int) value;
        if (targetType == long.class || targetType == Long.class) return (long) value;
        if (targetType == short.class || targetType == Short.class) return (short) value;
        if (targetType == byte.class || targetType == Byte.class) return (byte) value;
        return value;
    }
}
