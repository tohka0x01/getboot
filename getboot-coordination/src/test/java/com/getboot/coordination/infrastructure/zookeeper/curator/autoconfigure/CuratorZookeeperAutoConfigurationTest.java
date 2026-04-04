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
package com.getboot.coordination.infrastructure.zookeeper.curator.autoconfigure;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Curator ZooKeeper 自动配置测试。
 *
 * @author qiheng
 */
class CuratorZookeeperAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CuratorZookeeperAutoConfiguration.class));

    /**
     * 验证开启 ZooKeeper 且配置连接地址时会注册 CuratorFramework。
     */
    @Test
    void shouldRegisterCuratorFrameworkWhenEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "getboot.coordination.zookeeper.enabled=true",
                        "getboot.coordination.zookeeper.connect-string=127.0.0.1:2181",
                        "getboot.coordination.zookeeper.namespace=getboot"
                )
                .run(context -> {
                    assertTrue(context.containsBean("curatorFramework"));
                    CuratorFramework curatorFramework = context.getBean(CuratorFramework.class);
                    assertNotNull(curatorFramework);
                    assertEquals("getboot", curatorFramework.getNamespace());
                    assertEquals(CuratorFrameworkState.STARTED, curatorFramework.getState());
                });
    }

    /**
     * 验证关闭 ZooKeeper 能力时不会注册 CuratorFramework。
     */
    @Test
    void shouldSkipCuratorFrameworkWhenDisabled() {
        contextRunner
                .withPropertyValues("getboot.coordination.zookeeper.enabled=false")
                .run(context -> assertFalse(context.containsBean("curatorFramework")));
    }

    /**
     * 验证开启 ZooKeeper 但缺少连接地址时会启动失败。
     */
    @Test
    void shouldFailWhenConnectStringMissing() {
        contextRunner
                .withPropertyValues("getboot.coordination.zookeeper.enabled=true")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(
                            context.getStartupFailure().getMessage().contains(
                                    "getboot.coordination.zookeeper.connect-string must not be empty"
                            )
                    );
                });
    }
}
