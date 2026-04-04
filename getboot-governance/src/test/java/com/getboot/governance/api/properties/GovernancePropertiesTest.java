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
package com.getboot.governance.api.properties;

import com.getboot.governance.infrastructure.sentinel.autoconfigure.SentinelGovernanceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GovernanceProperties} 测试。
 *
 * @author qiheng
 */
class GovernancePropertiesTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SentinelGovernanceAutoConfiguration.class));

    /**
     * 验证治理配置能够正确绑定到配置模型。
     */
    @Test
    void shouldBindGovernanceProperties() {
        contextRunner
                .withPropertyValues(
                        "getboot.governance.enabled=true",
                        "getboot.governance.sentinel.enabled=true",
                        "getboot.governance.sentinel.eager=true",
                        "getboot.governance.sentinel.transport.dashboard=127.0.0.1:8858",
                        "getboot.governance.sentinel.transport.port=8720",
                        "getboot.governance.sentinel.filter.enabled=false",
                        "getboot.governance.sentinel.filter.order=-100",
                        "getboot.governance.sentinel.openfeign.enabled=true",
                        "getboot.governance.sentinel.rest-template.enabled=false",
                        "getboot.governance.sentinel.management.endpoint.enabled=false",
                        "getboot.governance.sentinel.management.health.enabled=false"
                )
                .run(context -> {
                    GovernanceProperties properties = context.getBean(GovernanceProperties.class);

                    assertTrue(properties.isEnabled());
                    assertTrue(properties.getSentinel().isEnabled());
                    assertTrue(properties.getSentinel().isEager());
                    assertEquals("127.0.0.1:8858", properties.getSentinel().getTransport().getDashboard());
                    assertEquals(8720, properties.getSentinel().getTransport().getPort());
                    assertFalse(properties.getSentinel().getFilter().isEnabled());
                    assertEquals(-100, properties.getSentinel().getFilter().getOrder());
                    assertTrue(properties.getSentinel().getOpenfeign().isEnabled());
                    assertFalse(properties.getSentinel().getRestTemplate().isEnabled());
                    assertFalse(properties.getSentinel().getManagement().getEndpoint().isEnabled());
                    assertFalse(properties.getSentinel().getManagement().getHealth().isEnabled());
                });
    }
}
