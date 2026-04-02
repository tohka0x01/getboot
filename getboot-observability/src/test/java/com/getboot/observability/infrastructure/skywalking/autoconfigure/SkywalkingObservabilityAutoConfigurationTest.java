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
package com.getboot.observability.infrastructure.skywalking.autoconfigure;

import com.getboot.observability.api.context.ReactiveTraceContext;
import com.getboot.observability.infrastructure.skywalking.support.SkywalkingTraceContextCustomizer;
import com.getboot.observability.infrastructure.skywalking.support.SkywalkingTraceIdResolver;
import com.getboot.observability.spi.ReactiveTraceContextCustomizer;
import com.getboot.observability.spi.ReactiveTraceIdResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkywalkingObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    SkywalkingObservabilityAutoConfiguration.class
            ))
            .withPropertyValues(
                    "getboot.observability.skywalking.enabled=true",
                    "getboot.observability.skywalking.mdc-key=swTraceId"
            );

    @Test
    void shouldExposeSkywalkingBeansForServletAndReactiveBridge() {
        org.apache.skywalking.apm.toolkit.trace.TraceContext.setTraceId("sw-trace-001");

        contextRunner.run(context -> {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/demo").build());

            SkywalkingTraceIdResolver traceIdResolver = context.getBean(SkywalkingTraceIdResolver.class);
            SkywalkingTraceContextCustomizer traceContextCustomizer =
                    context.getBean(SkywalkingTraceContextCustomizer.class);
            ReactiveTraceIdResolver reactiveTraceIdResolver = context.getBean(ReactiveTraceIdResolver.class);
            ReactiveTraceContextCustomizer reactiveTraceContextCustomizer =
                    context.getBean(ReactiveTraceContextCustomizer.class);

            assertEquals("sw-trace-001", traceIdResolver.resolve(exchange));
            assertEquals("sw-trace-001", reactiveTraceIdResolver.resolve(exchange));
            assertEquals(
                    Map.of("swTraceId", "sw-trace-001"),
                    traceContextCustomizer.customize(new ReactiveTraceContext("main-trace-id", exchange))
            );
            assertEquals(
                    Map.of("swTraceId", "sw-trace-001"),
                    reactiveTraceContextCustomizer.customize(new ReactiveTraceContext("main-trace-id", exchange))
            );
        });
    }
}
