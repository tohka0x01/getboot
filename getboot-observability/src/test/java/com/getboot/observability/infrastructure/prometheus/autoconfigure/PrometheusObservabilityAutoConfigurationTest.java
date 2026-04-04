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
package com.getboot.observability.infrastructure.prometheus.autoconfigure;

import com.getboot.observability.spi.prometheus.ObservabilityMeterRegistryCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prometheus 可观测性自动配置测试。
 */
class PrometheusObservabilityAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PrometheusObservabilityAutoConfiguration.class));

    /**
     * 验证公共标签和自定义 MeterRegistry 定制器都会生效。
     */
    @Test
    void shouldApplyCommonTagsAndInvokeCustomizers() {
        AtomicBoolean customized = new AtomicBoolean(false);

        contextRunner
                .withPropertyValues(
                        "getboot.observability.metrics.enabled=true",
                        "getboot.observability.metrics.common-tags.app=demo-service",
                        "getboot.observability.metrics.common-tags.env=prod"
                )
                .withBean(ObservabilityMeterRegistryCustomizer.class, () -> registry -> customized.set(true))
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    MeterRegistryCustomizer<MeterRegistry> customizer =
                            (MeterRegistryCustomizer<MeterRegistry>) context.getBean("getbootMeterRegistryCustomizer");
                    SimpleMeterRegistry registry = new SimpleMeterRegistry();

                    customizer.customize(registry);
                    registry.counter("demo.counter");

                    List<Tag> tags = registry.find("demo.counter").counter().getId().getTags();
                    assertEquals(List.of(Tag.of("app", "demo-service"), Tag.of("env", "prod")), tags);
                    assertTrue(customized.get());
                });
    }

    /**
     * 验证关闭 metrics 后不会注册 MeterRegistryCustomizer。
     */
    @Test
    void shouldSkipCustomizerWhenMetricsDisabled() {
        contextRunner
                .withPropertyValues("getboot.observability.metrics.enabled=false")
                .run(context -> assertFalse(context.containsBean("getbootMeterRegistryCustomizer")));
    }
}
