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
package com.getboot.auth.api.session;

/**
 * 登录会话操作门面。
 *
 * <p>用于统一收口登录、退出和 token 读取能力，避免业务侧直接耦合具体认证实现。</p>
 *
 * @author qiheng
 */
public interface LoginSessionOperator {

    /**
     * 建立登录会话。
     *
     * @param userId 登录用户 ID
     * @param userInfo 当前会话用户信息
     */
    void login(Long userId, Object userInfo);

    /**
     * 退出当前登录会话。
     */
    void logout();

    /**
     * 判断当前是否已登录。
     *
     * @return 是否已登录
     */
    boolean isLogin();

    /**
     * 获取当前 token 名称。
     *
     * @return token 名称
     */
    String getTokenName();

    /**
     * 获取当前 token 值。
     *
     * @return token 值
     */
    String getTokenValue();
}
