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
package com.getboot.auth.infrastructure.satoken.environment;

import com.getboot.support.infrastructure.environment.PropertyAliasEnvironmentPostProcessorSupport;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * Sa-Token 配置别名处理器。
 *
 * <p>用于将 GetBoot 风格的 Sa-Token 配置前缀映射为组件原生前缀。</p>
 *
 * @author qiheng
 */
public class SaTokenPropertyAliasEnvironmentPostProcessor extends PropertyAliasEnvironmentPostProcessorSupport {

    /**
     * 返回 Sa-Token 别名属性源名称。
     *
     * @return 属性源名称
     */
    @Override
    protected String aliasedPropertySourceName() {
        return "getbootAuthSatokenAliasedProperties";
    }

    /**
     * 注册 GetBoot Sa-Token 前缀到 Sa-Token 原生前缀的映射。
     *
     * @param environment 当前环境
     * @param aliasedProperties 别名属性容器
     */
    @Override
    protected void contributeAliases(ConfigurableEnvironment environment, Map<String, Object> aliasedProperties) {
        aliasPrefix(environment, aliasedProperties, "getboot.auth.satoken.", "sa-token.");
    }
}
