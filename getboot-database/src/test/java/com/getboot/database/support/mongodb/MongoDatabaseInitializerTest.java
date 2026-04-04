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
package com.getboot.database.support.mongodb;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.getboot.database.api.properties.DatabaseProperties;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;

/**
 * {@link MongoDatabaseInitializer} 测试。
 *
 * @author qiheng
 */
class MongoDatabaseInitializerTest {

    /**
     * 验证初始化阶段与启动后校验阶段都会执行 MongoDB ping。
     */
    @Test
    void shouldPingMongoDatabaseDuringInitialization() {
        TestMongoTemplate mongoTemplate = new TestMongoTemplate();

        DatabaseProperties.Init properties = new DatabaseProperties.Init();
        properties.setEnabled(true);
        properties.setStrictMode(true);

        MongoDatabaseInitializer initializer = new MongoDatabaseInitializer(mongoTemplate, properties);

        assertDoesNotThrow(initializer::init);
        initializer.validateAfterStartup(null);

        assertEquals(2, mongoTemplate.getPingCount());
    }

    /**
     * 验证严格模式下初始化失败会抛出异常。
     */
    @Test
    void shouldThrowWhenStrictModeAndInitializationFails() {
        TestMongoTemplate mongoTemplate = new TestMongoTemplate();
        mongoTemplate.failWith(new IllegalStateException("mongo down"));

        DatabaseProperties.Init properties = new DatabaseProperties.Init();
        properties.setEnabled(true);
        properties.setStrictMode(true);

        MongoDatabaseInitializer initializer = new MongoDatabaseInitializer(mongoTemplate, properties);

        assertThrows(RuntimeException.class, initializer::init);
    }

    /**
     * 轻量级 MongoTemplate 测试桩。
     */
    private static final class TestMongoTemplate extends MongoTemplate {

        /**
         * 测试用数据库实例。
         */
        private final MongoDatabase mongoDatabase = createMongoDatabase("demo");

        /**
         * ping 调用次数。
         */
        private int pingCount;

        /**
         * 预设失败异常。
         */
        private RuntimeException failure;

        /**
         * 创建测试用 MongoTemplate。
         */
        private TestMongoTemplate() {
            super(new TestMongoDatabaseFactory());
        }

        /**
         * 返回测试数据库实例。
         *
         * @return MongoDatabase
         */
        @Override
        public MongoDatabase getDb() {
            return mongoDatabase;
        }

        /**
         * 执行测试用 ping 命令。
         *
         * @param command 命令文档
         * @return 模拟结果
         */
        @Override
        public Document executeCommand(Document command) {
            if (failure != null) {
                throw failure;
            }
            pingCount++;
            return new Document("ok", 1);
        }

        /**
         * 记录需要抛出的异常。
         *
         * @param failure 预设异常
         */
        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        /**
         * 返回 ping 调用次数。
         *
         * @return ping 次数
         */
        private int getPingCount() {
            return pingCount;
        }
    }

    /**
     * 轻量级 MongoDatabaseFactory 测试桩。
     */
    private static final class TestMongoDatabaseFactory implements MongoDatabaseFactory {

        /**
         * 默认数据库实例。
         */
        private final MongoDatabase mongoDatabase = createMongoDatabase("demo");

        /**
         * 返回默认数据库实例。
         *
         * @return 数据库实例
         */
        @Override
        public MongoDatabase getMongoDatabase() {
            return mongoDatabase;
        }

        /**
         * 返回指定名称的数据库实例。
         *
         * @param dbName 数据库名称
         * @return 数据库实例
         */
        @Override
        public MongoDatabase getMongoDatabase(String dbName) {
            return createMongoDatabase(dbName);
        }

        /**
         * 返回空异常转换器。
         *
         * @return 异常转换器
         */
        @Override
        public PersistenceExceptionTranslator getExceptionTranslator() {
            return exception -> null;
        }

        /**
         * 当前测试场景不需要 Session。
         *
         * @param options Session 选项
         * @return 不返回
         */
        @Override
        public ClientSession getSession(ClientSessionOptions options) {
            throw new UnsupportedOperationException("Session is not required in tests.");
        }

        /**
         * 当前测试场景忽略 Session 绑定。
         *
         * @param session 客户端 Session
         * @return 当前工厂
         */
        @Override
        public MongoDatabaseFactory withSession(ClientSession session) {
            return this;
        }
    }

    /**
     * 构造测试数据库代理，提供最小必要行为。
     *
     * @param databaseName 数据库名称
     * @return 数据库代理
     */
    private static MongoDatabase createMongoDatabase(String databaseName) {
        return (MongoDatabase) Proxy.newProxyInstance(
                MongoDatabase.class.getClassLoader(),
                new Class[]{MongoDatabase.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> databaseName;
                    case "getCodecRegistry" -> MongoClientSettings.getDefaultCodecRegistry();
                    case "withCodecRegistry", "withReadPreference", "withReadConcern", "withWriteConcern" -> proxy;
                    case "toString" -> "TestMongoDatabase(" + databaseName + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
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
    private static Object defaultValue(Class<?> returnType) {
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
