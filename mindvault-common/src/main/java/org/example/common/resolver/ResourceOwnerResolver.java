package org.example.common.resolver;

import org.example.common.exception.BusinessException;

public interface ResourceOwnerResolver {

    /**
     * 判断这个 resolver 是否支持该资源类型
     */
    boolean supports(String resourceType);

    /**
     * 根据资源ID查询资源所有者的用户ID
     * @throws BusinessException 如果资源不存在
     */
    Long getOwnerId(Long resourceId, String resourceType);
}