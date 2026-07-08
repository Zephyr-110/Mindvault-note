package org.example.common.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.example.common.exception.BusinessException;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class AspectUtil {

    /**
     * 缓存反射获取的Field对象：key = (dto.getClass(), fieldName)
     * 避免每次请求都重复反射
     */
    private static final ConcurrentHashMap<CacheKey, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 从切点参数中提取第一个非基本类型参数（通常是DTO）
     */
    public static Object extractDTO(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null && !isBasicType(arg)) {
                return arg;
            }
        }
        throw new BusinessException(500, "切面无法定位DTO参数");
    }

    /**
     * 通过反射从DTO中获取指定字段的Long值，第一次反射后缓存Field对象
     */
    public static Long getResourceId(Object dto, String fieldName) {
        try {
            CacheKey key = new CacheKey(dto.getClass(), fieldName);
            Field field = FIELD_CACHE.computeIfAbsent(key, k -> {
                try {
                    Field f = k.clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException e) {
                    throw new BusinessException(500, "找不到资源ID字段: " + fieldName);
                }
            });
            Object value = field.get(dto);
            return value == null ? null : ((Number) value).longValue();
        } catch (Exception e) {
            throw new BusinessException(500, "获取资源ID失败: " + fieldName);
        }
    }

    /**
     * 缓存Key：组合 Class + 字段名
     */
    private record CacheKey(Class<?> clazz, String fieldName) {}

    /**
     * 判断参数是否为基本类型
     */
    public static boolean isBasicType(Object arg) {
        return arg.getClass().isPrimitive()
                || arg instanceof Number
                || arg instanceof String
                || arg instanceof Boolean
                || arg instanceof Character;
    }
}