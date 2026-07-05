package org.example.common.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.example.common.exception.BusinessException;

import java.lang.reflect.Field;

public class AspectUtil {

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
     * 通过反射从DTO中获取指定字段的Long值
     */
    public static Long getResourceId(Object dto, String fieldName) {
        try {
            Field field = dto.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(dto);
            return value == null ? null : ((Number) value).longValue();
        } catch (Exception e) {
            throw new BusinessException(500, "找不到资源ID字段: " + fieldName);
        }
    }

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