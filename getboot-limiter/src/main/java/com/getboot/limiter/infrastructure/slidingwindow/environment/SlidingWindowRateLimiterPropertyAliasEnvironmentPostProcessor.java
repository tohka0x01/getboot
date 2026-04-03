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
package com.getboot.limiter.infrastructure.slidingwindow.environment;

import com.getboot.support.infrastructure.environment.PropertyAliasEnvironmentPostProcessorSupport;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * limiter 配置别名处理器。
 *
 * <p>将旧版 `getboot.limiter.*` 配置映射到新的 `getboot.limiter.sliding-window.*` 子树，避免存量项目立即改配置。</p>
 *
 * @author qiheng
 */
public class SlidingWindowRateLimiterPropertyAliasEnvironmentPostProcessor
        extends PropertyAliasEnvironmentPostProcessorSupport {

    /**
     * 返回别名属性源名称。
     *
     * @return 属性源名称
     */
    @Override
    protected String aliasedPropertySourceName() {
        return "getbootLimiterSlidingWindowAliasedProperties";
    }

    /**
     * 写入旧配置到新配置的别名映射。
     *
     * @param environment Spring 环境
     * @param aliasedProperties 别名属性集合
     */
    @Override
    protected void contributeAliases(ConfigurableEnvironment environment, Map<String, Object> aliasedProperties) {
        aliasPrefix(
                environment,
                aliasedProperties,
                "getboot.limiter.limiters.",
                "getboot.limiter.sliding-window.limiters."
        );
        aliasProperty(environment, aliasedProperties,
                "getboot.limiter.default-timeout",
                "getboot.limiter.sliding-window.default-timeout");
        aliasProperty(environment, aliasedProperties,
                "getboot.limiter.key-prefix",
                "getboot.limiter.sliding-window.key-prefix");
    }

    /**
     * 在目标属性未显式配置时写入单个别名属性。
     *
     * @param environment Spring 环境
     * @param aliasedProperties 别名属性集合
     * @param sourcePropertyName 旧属性名
     * @param targetPropertyName 新属性名
     */
    private void aliasProperty(ConfigurableEnvironment environment,
                               Map<String, Object> aliasedProperties,
                               String sourcePropertyName,
                               String targetPropertyName) {
        if (!environment.containsProperty(sourcePropertyName) || environment.containsProperty(targetPropertyName)) {
            return;
        }
        aliasedProperties.putIfAbsent(targetPropertyName, environment.getProperty(sourcePropertyName));
    }
}
