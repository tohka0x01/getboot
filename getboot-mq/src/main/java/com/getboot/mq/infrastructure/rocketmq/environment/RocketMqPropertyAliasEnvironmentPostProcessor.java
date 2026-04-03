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
package com.getboot.mq.infrastructure.rocketmq.environment;

import com.getboot.support.infrastructure.environment.PropertyAliasEnvironmentPostProcessorSupport;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

/**
 * RocketMQ 配置别名处理器。
 *
 * <p>用于将 GetBoot 风格的 RocketMQ 配置前缀映射为底层组件原生前缀。</p>
 *
 * @author qiheng
 */
public class RocketMqPropertyAliasEnvironmentPostProcessor extends PropertyAliasEnvironmentPostProcessorSupport {

    /**
     * 返回 RocketMQ 配置桥接属性源名称。
     *
     * @return RocketMQ 配置桥接属性源名称
     */
    @Override
    protected String aliasedPropertySourceName() {
        return "getbootMqRocketMqAliasedProperties";
    }

    /**
     * 将 GetBoot 风格的 RocketMQ 配置桥接为底层 RocketMQ 原生配置。
     *
     * @param environment 当前环境
     * @param aliasedProperties 待写入的别名属性集合
     */
    @Override
    protected void contributeAliases(ConfigurableEnvironment environment, Map<String, Object> aliasedProperties) {
        aliasPrefix(environment, aliasedProperties, "getboot.mq.rocketmq.", "rocketmq.");
    }
}
