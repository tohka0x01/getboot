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
package com.getboot.auth.infrastructure.satoken.servlet;

import cn.dev33.satoken.stp.StpUtil;
import com.getboot.auth.spi.SaTokenServletAuthChecker;

/**
 * 默认的 Sa-Token Servlet 认证校验器。
 *
 * <p>默认只校验登录态，具体权限模型由业务方按需覆盖。</p>
 *
 * @author qiheng
 */
public class DefaultSaTokenServletAuthChecker implements SaTokenServletAuthChecker {

    /**
     * 执行默认登录态校验。
     */
    @Override
    public void check() {
        StpUtil.checkLogin();
    }
}
