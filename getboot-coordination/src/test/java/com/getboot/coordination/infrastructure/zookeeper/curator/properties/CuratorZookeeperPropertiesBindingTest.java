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
package com.getboot.coordination.infrastructure.zookeeper.curator.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CuratorZookeeperProperties} 绑定测试。
 *
 * @author qiheng
 */
class CuratorZookeeperPropertiesBindingTest {

    /**
     * 验证 kebab-case 配置能够绑定到 Curator ZooKeeper 属性。
     */
    @Test
    void shouldBindCuratorZookeeperPropertiesFromKebabCaseConfiguration() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("getboot.coordination.zookeeper.enabled", "true");
        source.put("getboot.coordination.zookeeper.connect-string", "127.0.0.1:2181");
        source.put("getboot.coordination.zookeeper.namespace", "getboot");
        source.put("getboot.coordination.zookeeper.session-timeout-ms", "45000");
        source.put("getboot.coordination.zookeeper.connection-timeout-ms", "5000");
        source.put("getboot.coordination.zookeeper.retry.base-sleep-time-ms", "1500");
        source.put("getboot.coordination.zookeeper.retry.max-retries", "5");
        source.put("getboot.coordination.zookeeper.retry.max-sleep-ms", "9000");

        CuratorZookeeperProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("getboot.coordination.zookeeper", Bindable.of(CuratorZookeeperProperties.class))
                .orElseThrow(() -> new IllegalStateException("curator properties should bind"));

        assertTrue(properties.isEnabled());
        assertEquals("127.0.0.1:2181", properties.getConnectString());
        assertEquals("getboot", properties.getNamespace());
        assertEquals(45000, properties.getSessionTimeoutMs());
        assertEquals(5000, properties.getConnectionTimeoutMs());
        assertEquals(1500, properties.getRetry().getBaseSleepTimeMs());
        assertEquals(5, properties.getRetry().getMaxRetries());
        assertEquals(9000, properties.getRetry().getMaxSleepMs());
    }
}
