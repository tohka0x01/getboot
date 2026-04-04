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
package com.getboot.database.infrastructure.datasource.autoconfigure;

import com.getboot.database.api.properties.DatabaseProperties;
import com.getboot.database.support.datasource.DataSourceInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据源自动配置测试。
 *
 * @author qiheng
 */
class DataSourceAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

    /**
     * 验证数据库增强开启且存在 DataSource 时会注册数据源初始化器。
     */
    @Test
    void shouldRegisterInitializerWhenDatabaseEnabledAndDataSourcePresent() {
        contextRunner
                .withPropertyValues("getboot.database.enabled=true")
                .withBean(DataSource.class, TestDataSource::new)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.containsBean("dataSourceInitializer"));
                    assertNotNull(context.getBean(DataSourceInitializer.class));
                });
    }

    /**
     * 验证模块总开关关闭时不会注册数据源初始化器。
     */
    @Test
    void shouldSkipInitializerWhenDatabaseModuleDisabled() {
        contextRunner
                .withPropertyValues("getboot.database.enabled=false")
                .withBean(DataSource.class, TestDataSource::new)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertFalse(context.containsBean("dataSourceInitializer"));
                });
    }

    /**
     * 验证关闭数据源预热时不会注册初始化器。
     */
    @Test
    void shouldSkipInitializerWhenDatasourceInitDisabled() {
        contextRunner
                .withPropertyValues(
                        "getboot.database.enabled=true",
                        "getboot.database.datasource.init.enabled=false"
                )
                .withBean(DataSource.class, TestDataSource::new)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertFalse(context.containsBean("dataSourceInitializer"));
                });
    }

    /**
     * 验证业务方自定义数据源初始化器时自动配置会回退。
     */
    @Test
    void shouldBackOffWhenCustomInitializerProvided() {
        DataSourceInitializer customInitializer = new DataSourceInitializer(new TestDataSource(), disabledInitProperties());

        contextRunner
                .withPropertyValues("getboot.database.enabled=true")
                .withBean(DataSource.class, TestDataSource::new)
                .withBean(DataSourceInitializer.class, () -> customInitializer)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertSame(customInitializer, context.getBean(DataSourceInitializer.class));
                    assertEquals(1, context.getBeansOfType(DataSourceInitializer.class).size());
                });
    }

    /**
     * 验证缺少 DataSource Bean 时不会错误创建初始化器。
     */
    @Test
    void shouldSkipInitializerWhenDataSourceMissing() {
        contextRunner
                .withPropertyValues("getboot.database.enabled=true")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertFalse(context.containsBean("dataSourceInitializer"));
                });
    }

    /**
     * 返回关闭初始化逻辑的配置，避免自定义 Bean 在创建时触发真实连接。
     *
     * @return 初始化配置
     */
    private DatabaseProperties.Init disabledInitProperties() {
        DatabaseProperties.Init properties = new DatabaseProperties.Init();
        properties.setEnabled(false);
        properties.setValidateAfterStartup(false);
        return properties;
    }

    /**
     * 轻量级测试数据源，避免依赖 Mockito 与外部数据库。
     */
    private static final class TestDataSource implements DataSource {

        /**
         * 返回连接代理。
         *
         * @return 测试连接
         */
        @Override
        public Connection getConnection() {
            return createConnection();
        }

        /**
         * 返回连接代理。
         *
         * @param username 用户名
         * @param password 密码
         * @return 测试连接
         */
        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        /**
         * 当前测试不维护日志输出器。
         *
         * @return 空
         */
        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        /**
         * 忽略日志输出器设置。
         *
         * @param out 日志输出器
         */
        @Override
        public void setLogWriter(PrintWriter out) {
        }

        /**
         * 忽略登录超时设置。
         *
         * @param seconds 超时秒数
         */
        @Override
        public void setLoginTimeout(int seconds) {
        }

        /**
         * 返回默认登录超时。
         *
         * @return 零
         */
        @Override
        public int getLoginTimeout() {
            return 0;
        }

        /**
         * 返回默认父日志器。
         *
         * @return 全局日志器
         */
        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        /**
         * 当前数据源不支持包装展开。
         *
         * @param iface 目标类型
         * @param <T> 泛型类型
         * @return 不返回
         * @throws SQLException 总是抛出
         */
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Test datasource does not support unwrap.");
        }

        /**
         * 当前数据源不支持包装判断。
         *
         * @param iface 目标类型
         * @return false
         */
        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        /**
         * 构造连接代理，只实现测试需要的方法。
         *
         * @return 连接代理
         */
        private Connection createConnection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getMetaData" -> createMetaData();
                        case "createStatement" -> createStatement();
                        case "close" -> null;
                        case "isClosed" -> false;
                        case "toString" -> "TestConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "unwrap" -> throw new SQLException("Test connection does not support unwrap.");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        /**
         * 构造元数据代理，提供数据库基础信息。
         *
         * @return 元数据代理
         */
        private DatabaseMetaData createMetaData() {
            return (DatabaseMetaData) Proxy.newProxyInstance(
                    DatabaseMetaData.class.getClassLoader(),
                    new Class[]{DatabaseMetaData.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getDatabaseProductName" -> "H2";
                        case "getDatabaseProductVersion" -> "2.2.224";
                        case "toString" -> "TestDatabaseMetaData";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "unwrap" -> throw new SQLException("Test metadata does not support unwrap.");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        /**
         * 构造语句代理，兼容可能的启动后校验调用。
         *
         * @return 语句代理
         */
        private Statement createStatement() {
            return (Statement) Proxy.newProxyInstance(
                    Statement.class.getClassLoader(),
                    new Class[]{Statement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "execute" -> true;
                        case "close" -> null;
                        case "toString" -> "TestStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "unwrap" -> throw new SQLException("Test statement does not support unwrap.");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        /**
         * 返回常见返回类型的默认值。
         *
         * @param returnType 返回类型
         * @return 默认值
         */
        private Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (boolean.class == returnType) {
                return false;
            }
            if (byte.class == returnType) {
                return (byte) 0;
            }
            if (short.class == returnType) {
                return (short) 0;
            }
            if (int.class == returnType) {
                return 0;
            }
            if (long.class == returnType) {
                return 0L;
            }
            if (float.class == returnType) {
                return 0F;
            }
            if (double.class == returnType) {
                return 0D;
            }
            if (char.class == returnType) {
                return '\0';
            }
            return null;
        }
    }
}
