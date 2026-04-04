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
package com.getboot.database.infrastructure.environment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link DatabasePropertyAliasEnvironmentPostProcessor} 测试。
 *
 * @author qiheng
 */
class DatabasePropertyAliasEnvironmentPostProcessorTest {

    /**
     * 验证 GetBoot 数据库前缀会桥接到下游组件原生前缀，并保留模块内控制开关。
     */
    @Test
    void shouldAliasDatabasePropertiesToNativePrefixesWithoutLeakingModuleFlags() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.ofEntries(
                Map.entry("getboot.database.datasource.url", "jdbc:mysql://127.0.0.1:3306/demo"),
                Map.entry("getboot.database.datasource.username", "demo"),
                Map.entry("getboot.database.datasource.enabled", "false"),
                Map.entry("getboot.database.datasource.init.enabled", "false"),
                Map.entry("getboot.database.mongodb.uri", "mongodb://127.0.0.1:27017/demo"),
                Map.entry("getboot.database.mongodb.auto-index-creation", "true"),
                Map.entry("getboot.database.mongodb.enabled", "true"),
                Map.entry("getboot.database.mongodb.init.strict-mode", "false"),
                Map.entry("getboot.database.mybatis-plus.configuration.map-underscore-to-camel-case", "true"),
                Map.entry("getboot.database.sharding.props.sql-show", "true"),
                Map.entry("getboot.database.sharding.mode.type", "Memory"),
                Map.entry("getboot.database.sharding.rules.sharding.tables.t_order.actual-data-nodes",
                        "ds$->{0..1}.t_order_$->{0..1}"),
                Map.entry("getboot.database.sharding.datasource.names", "ds0,ds1")
        )));

        DatabasePropertyAliasEnvironmentPostProcessor processor = new DatabasePropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("jdbc:mysql://127.0.0.1:3306/demo", environment.getProperty("spring.datasource.url"));
        assertEquals("demo", environment.getProperty("spring.datasource.username"));
        assertEquals("mongodb://127.0.0.1:27017/demo", environment.getProperty("spring.data.mongodb.uri"));
        assertEquals("true", environment.getProperty("spring.data.mongodb.auto-index-creation"));
        assertEquals("true", environment.getProperty("mybatis-plus.configuration.map-underscore-to-camel-case"));
        assertEquals("true", environment.getProperty("spring.shardingsphere.props.sql-show"));
        assertEquals("Memory", environment.getProperty("spring.shardingsphere.mode.type"));
        assertEquals(
                "ds$->{0..1}.t_order_$->{0..1}",
                environment.getProperty("spring.shardingsphere.rules.sharding.tables.t_order.actual-data-nodes")
        );
        assertEquals("ds0,ds1", environment.getProperty("spring.shardingsphere.datasource.names"));

        assertNull(environment.getProperty("spring.datasource.enabled"));
        assertNull(environment.getProperty("spring.datasource.init.enabled"));
        assertNull(environment.getProperty("spring.data.mongodb.enabled"));
        assertNull(environment.getProperty("spring.data.mongodb.init.strict-mode"));
    }

    /**
     * 验证已显式声明的原生配置不会被 GetBoot 别名桥接覆盖。
     */
    @Test
    void shouldNotOverrideExistingNativeProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.database.datasource.url", "jdbc:mysql://127.0.0.1:3306/demo",
                "spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/keep",
                "getboot.database.mongodb.uri", "mongodb://127.0.0.1:27017/demo",
                "spring.data.mongodb.uri", "mongodb://127.0.0.1:27017/keep",
                "getboot.database.mybatis-plus.configuration.map-underscore-to-camel-case", "true",
                "mybatis-plus.configuration.map-underscore-to-camel-case", "false",
                "getboot.database.sharding.props.sql-show", "true",
                "spring.shardingsphere.props.sql-show", "false"
        )));

        DatabasePropertyAliasEnvironmentPostProcessor processor = new DatabasePropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("jdbc:mysql://127.0.0.1:3306/keep", environment.getProperty("spring.datasource.url"));
        assertEquals("mongodb://127.0.0.1:27017/keep", environment.getProperty("spring.data.mongodb.uri"));
        assertEquals("false", environment.getProperty("mybatis-plus.configuration.map-underscore-to-camel-case"));
        assertEquals("false", environment.getProperty("spring.shardingsphere.props.sql-show"));
    }
}
