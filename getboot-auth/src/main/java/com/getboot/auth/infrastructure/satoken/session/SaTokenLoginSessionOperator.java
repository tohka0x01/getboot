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
package com.getboot.auth.infrastructure.satoken.session;

import cn.dev33.satoken.stp.StpUtil;
import com.getboot.auth.api.session.LoginSessionOperator;
import com.getboot.auth.infrastructure.satoken.support.SaTokenSessionConstants;

import java.util.Objects;

/**
 * Sa-Token 登录会话操作器。
 *
 * <p>统一封装登录态建立、退出和 token 读取，避免业务侧直接依赖 Sa-Token 细节。</p>
 *
 * @author qiheng
 */
public class SaTokenLoginSessionOperator implements LoginSessionOperator {

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(Long userId, Object userInfo) {
        StpUtil.login(Objects.requireNonNull(userId, "userId must not be null"));
        if (userInfo != null) {
            StpUtil.getSession().set(SaTokenSessionConstants.USER_INFO_SESSION_KEY, userInfo);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenName() {
        return StpUtil.getTokenName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenValue() {
        return StpUtil.getTokenValue();
    }
}
