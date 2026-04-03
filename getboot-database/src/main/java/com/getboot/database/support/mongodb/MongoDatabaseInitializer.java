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

import com.getboot.database.api.properties.DatabaseProperties;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 启动初始化器。
 *
 * <p>用于在应用启动阶段提前执行 ping 校验，并在应用完成启动后再次验证连通性。</p>
 *
 * @author qiheng
 */
public class MongoDatabaseInitializer {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(MongoDatabaseInitializer.class);

    /**
     * MongoTemplate 实例。
     */
    private final MongoTemplate mongoTemplate;

    /**
     * 初始化配置。
     */
    private final DatabaseProperties.Init properties;

    /**
     * 创建 MongoDB 初始化器。
     *
     * @param mongoTemplate MongoTemplate 实例
     * @param properties 初始化配置
     */
    public MongoDatabaseInitializer(MongoTemplate mongoTemplate, DatabaseProperties.Init properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    /**
     * 在 Bean 初始化后立即执行 MongoDB 预热。
     */
    @PostConstruct
    @Order(1)
    public void init() {
        if (!properties.isEnabled()) {
            log.info("Eager MongoDB initialization is disabled.");
            return;
        }
        log.info("Starting MongoDB initialization.");
        long startTime = System.currentTimeMillis();
        try {
            Document result = ping();
            long costTime = System.currentTimeMillis() - startTime;
            log.info("MongoDB initialized successfully. database={}, result={}, cost={}ms",
                    mongoTemplate.getDb().getName(), result.toJson(), costTime);
        } catch (Exception exception) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("MongoDB initialization failed. cost={}ms", costTime, exception);
            if (properties.isStrictMode()) {
                throw new RuntimeException("MongoDB initialization failed during startup.", exception);
            }
            log.warn("MongoDB initialization failed, but the application will continue in non-strict mode.");
        }
    }

    /**
     * 在应用启动完成后执行一次 MongoDB 连通性校验。
     *
     * @param event 应用启动完成事件
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void validateAfterStartup(ApplicationReadyEvent event) {
        log.info("Received MongoDB validation event: {}", event);
        if (!properties.isEnabled() || !properties.isValidateAfterStartup()) {
            return;
        }
        log.info("Validating MongoDB connectivity after application startup.");
        try {
            Document result = ping();
            log.info("MongoDB validation after startup succeeded. database={}, result={}",
                    mongoTemplate.getDb().getName(), result.toJson());
        } catch (Exception exception) {
            log.warn("MongoDB validation after startup failed: {}", exception.getMessage());
        }
    }

    /**
     * 执行 MongoDB ping 命令。
     *
     * @return ping 结果
     */
    private Document ping() {
        return mongoTemplate.executeCommand(new Document("ping", 1));
    }
}
