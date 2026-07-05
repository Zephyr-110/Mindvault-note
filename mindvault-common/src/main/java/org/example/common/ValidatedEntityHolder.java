package org.example.common;

import java.util.HashMap;
import java.util.Map;

public class ValidatedEntityHolder {
    private static final ThreadLocal<Map<Class<?>, Object>> HOLDER = ThreadLocal.withInitial(HashMap::new);

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) HOLDER.get().get(clazz);
    }

    public static <T> void set(T entity) {
        HOLDER.get().put(entity.getClass(), entity);
    }

    public static void clear() {
        HOLDER.remove();
    }
}