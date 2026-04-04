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
package com.getboot.storage.infrastructure.autoconfigure;

import com.getboot.storage.api.model.StoragePresignMethod;
import com.getboot.storage.api.properties.StorageProperties;
import com.getboot.storage.spi.StorageBucketRouter;
import com.getboot.storage.spi.StorageObjectKeyGenerator;
import com.getboot.storage.support.DefaultStorageBucketRouter;
import com.getboot.storage.support.DefaultStorageObjectKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对象存储自动配置测试。
 *
 * @author qiheng
 */
class StorageAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StorageAutoConfiguration.class));

    /**
     * 验证会注册核心 Bean，并完成 TTL 与 bucket 路由配置绑定。
     */
    @Test
    void shouldRegisterCoreBeansAndBindStorageProperties() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=true",
                        "getboot.storage.default-bucket=app-default",
                        "getboot.storage.default-download-url-ttl=20m",
                        "getboot.storage.default-upload-url-ttl=5m",
                        "getboot.storage.scene-buckets.invoice=invoice-bucket"
                )
                .run(context -> {
                    assertInstanceOf(DefaultStorageBucketRouter.class, context.getBean(StorageBucketRouter.class));
                    assertInstanceOf(DefaultStorageObjectKeyGenerator.class,
                            context.getBean(StorageObjectKeyGenerator.class));

                    StorageProperties properties = context.getBean(StorageProperties.class);
                    assertEquals(Duration.ofMinutes(20), properties.resolveDefaultTtl(StoragePresignMethod.DOWNLOAD));
                    assertEquals(Duration.ofMinutes(5), properties.resolveDefaultTtl(StoragePresignMethod.UPLOAD));
                    assertEquals("invoice-bucket", properties.getSceneBuckets().get("invoice"));

                    StorageBucketRouter bucketRouter = context.getBean(StorageBucketRouter.class);
                    assertEquals("invoice-bucket", bucketRouter.resolveBucket("invoice", null));
                    assertEquals("app-default", bucketRouter.resolveBucket("unknown", null));
                    assertFalse(context.containsBean("minioClient"));
                    assertFalse(context.containsBean("storageOperator"));
                });
    }

    /**
     * 验证自定义核心 Bean 存在时，默认实现会让位。
     */
    @Test
    void shouldBackOffWhenCustomCoreBeansProvided() {
        StorageBucketRouter customBucketRouter = (scene, requestedBucket) -> "custom-bucket";
        StorageObjectKeyGenerator customObjectKeyGenerator =
                (scene, requestedObjectKey, originalFilename) -> "custom-key";

        contextRunner
                .withPropertyValues("getboot.storage.enabled=true")
                .withBean(StorageBucketRouter.class, () -> customBucketRouter)
                .withBean(StorageObjectKeyGenerator.class, () -> customObjectKeyGenerator)
                .run(context -> {
                    assertSame(customBucketRouter, context.getBean(StorageBucketRouter.class));
                    assertSame(customObjectKeyGenerator, context.getBean(StorageObjectKeyGenerator.class));
                });
    }

    /**
     * 验证禁用 MinIO 实现时仍保留核心 Bean，但跳过 MinIO 相关装配。
     */
    @Test
    void shouldSkipMinioBeansWhenMinioIsDisabled() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=true",
                        "getboot.storage.default-bucket=app-default",
                        "getboot.storage.minio.enabled=false",
                        "getboot.storage.minio.endpoint=http://127.0.0.1:9000",
                        "getboot.storage.minio.access-key=minioadmin",
                        "getboot.storage.minio.secret-key=minioadmin"
                )
                .run(context -> {
                    assertTrue(context.containsBean("storageBucketRouter"));
                    assertTrue(context.containsBean("storageObjectKeyGenerator"));
                    assertFalse(context.containsBean("minioClient"));
                    assertFalse(context.containsBean("storageOperator"));
                });
    }

    /**
     * 验证切换为非 MinIO 类型时不会注册 MinIO 客户端与门面。
     */
    @Test
    void shouldSkipMinioBeansWhenStorageTypeIsNotMinio() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=true",
                        "getboot.storage.type=s3",
                        "getboot.storage.default-bucket=app-default",
                        "getboot.storage.minio.endpoint=http://127.0.0.1:9000",
                        "getboot.storage.minio.access-key=minioadmin",
                        "getboot.storage.minio.secret-key=minioadmin"
                )
                .run(context -> {
                    assertTrue(context.containsBean("storageBucketRouter"));
                    assertTrue(context.containsBean("storageObjectKeyGenerator"));
                    assertFalse(context.containsBean("minioClient"));
                    assertFalse(context.containsBean("storageOperator"));
                });
    }
}
