package org.example.common.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.CheckOwner;
import org.example.common.annotation.CheckResource;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.example.common.resolver.ResourceOwnerResolver;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class CheckOwnerAspect {

    private final List<ResourceOwnerResolver> resolvers;


    @Around("@annotation(checkOwner)")
    public Object checkOwner(ProceedingJoinPoint joinPoint, CheckOwner checkOwner) throws Throwable {
        //提取到参数
        Object dto = extractDTO(joinPoint);
        //
        for (CheckResource resource : checkOwner.value()) {
            //校验资源所有者是否是当前用户
            validateResource(dto, resource, checkOwner.errorMessage());
        }
        return joinPoint.proceed();
    }

    /**
     *
     * 提取第一个非基本类型参数
     */
    private Object extractDTO(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null && !isBasicType(arg)) {
                return arg;
            }
        }
        throw new BusinessException(500, "切面无法定位DTO参数");
    }

    /**
     *
     * 校验资源所有者是否是当前用户
     */
    private void validateResource(Object dto, CheckResource resource, String errorMessage) {
        //获取资源ID
        Long resourceId = getResourceId(dto, resource.idParam());
        //判断资源ID是否为空
        if (resourceId == null) {
            return;
        }
        //找支持该类型的 resolver
        ResourceOwnerResolver resolver = findResolver(resource.type());
        //获取资源所有者的用户ID
        Long ownerUserId = resolver.getOwnerId(resourceId, resource.type());
        //判断资源所有者是否是当前用户
        Long currentUserId = UserContext.getUserId();
        if (!ownerUserId.equals(currentUserId)) {
            throw new BusinessException(403, errorMessage);
        }
    }

    /**
     *
     * 找到支持该类型的 resolver
     */
    private ResourceOwnerResolver findResolver(String resourceType) {
        return resolvers.stream()
                //过滤出支持该类型的 resolver
                .filter(r -> r.supports(resourceType))
                //取第一个
                .findFirst()
                //如果没有，抛异常
                .orElseThrow(() -> new BusinessException(500, "不支持该资源类型: " + resourceType));
    }

    /**
     *
     * 获取资源ID
     */
    private Long getResourceId(Object dto, String fieldName) {
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
     *
     * 判断是否基础类型
     */
    private boolean isBasicType(Object arg) {
        return arg.getClass().isPrimitive()
                || arg instanceof Number
                || arg instanceof String
                || arg instanceof Boolean
                || arg instanceof Character;
    }
}