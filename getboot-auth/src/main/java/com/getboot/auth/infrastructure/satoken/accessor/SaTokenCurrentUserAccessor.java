/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getboot.auth.infrastructure.satoken.accessor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.getboot.auth.api.accessor.CurrentUserAccessor;
import com.getboot.auth.infrastructure.satoken.support.SaTokenSessionConstants;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;

/**
 * Sa-Token 当前用户访问器实现。
 *
 * <p>用于从 Sa-Token 会话中获取当前登录用户对象及用户标识。</p>
 *
 * @author qiheng
 */
public class SaTokenCurrentUserAccessor implements CurrentUserAccessor {

    /**
     * 读取当前登录用户对象。
     *
     * @param userType 目标用户类型
     * @param <T> 用户对象类型
     * @return 当前登录用户对象
     */
    @Override
    public <T> T getCurrentUser(Class<T> userType) {
        Object userInfo;
        try {
            userInfo = StpUtil.getSession().get(SaTokenSessionConstants.USER_INFO_SESSION_KEY);
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        }
        if (userInfo == null) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        }
        if (userType.isInstance(userInfo)) {
            return userType.cast(userInfo);
        }
        try {
            return JSONUtil.toBean(JSONUtil.toJsonStr(userInfo), userType);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to convert current user session data.", exception);
        }
    }

    /**
     * 读取当前登录用户 ID。
     *
     * @return 当前登录用户 ID
     */
    @Override
    public Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception exception) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        }
    }
}
