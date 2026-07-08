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
import org.example.common.util.AspectUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Aspect
@Order(2)
@RequiredArgsConstructor
public class CheckOwnerAspect {

    private final List<ResourceOwnerResolver> resolvers;


    @Around("@annotation(checkOwner)")
    public Object checkOwner(ProceedingJoinPoint joinPoint, CheckOwner checkOwner) throws Throwable {
        //拿到切点参数
        Object dto = AspectUtil.extractDTO(joinPoint);
        //校验资源所有者是否为当前用户
        for (CheckResource resource : checkOwner.value()) {
            validateResource(dto, resource, checkOwner.errorMessage());
        }
        //校验通过，放行
        return joinPoint.proceed();
    }


    /**
     * 校验资源所有者是否为当前用户
     * @param dto 切点参数
     * @param resource 校验的资源类型
     * @param errorMessage 校验失败时的错误信息
     */
    private void validateResource(Object dto, CheckResource resource, String errorMessage) {
        Long resourceId = AspectUtil.getResourceId(dto, resource.idParam());
        if (resourceId == null) {
            return;
        }
        ResourceOwnerResolver resolver = findResolver(resource.type());
        Long ownerUserId = resolver.getOwnerId(resourceId, resource.type());
        Long currentUserId = UserContext.getUserId();
        if (!ownerUserId.equals(currentUserId)) {
            throw new BusinessException(403, errorMessage);
        }
    }

    /**
     * 根据资源类型找到对应的解析器
     * @param resourceType 资源类型
     * @return 对应的解析器
     * @throws BusinessException 如果没有对应的解析器
     */
    private ResourceOwnerResolver findResolver(String resourceType) {
        return resolvers.stream()
                .filter(r -> r.supports(resourceType))
                .findFirst()
                .orElseThrow(() -> new BusinessException(500, "不支持该资源类型: " + resourceType));
    }
}