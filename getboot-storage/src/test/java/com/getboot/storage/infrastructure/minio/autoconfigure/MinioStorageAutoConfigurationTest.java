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
package com.getboot.storage.infrastructure.minio.autoconfigure;

import com.getboot.storage.api.operator.StorageOperator;
import com.getboot.storage.infrastructure.autoconfigure.StorageAutoConfiguration;
import com.getboot.storage.infrastructure.minio.support.MinioStorageOperator;
import com.getboot.storage.spi.StorageBucketRouter;
import com.getboot.storage.spi.StorageObjectKeyGenerator;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioStorageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StorageAutoConfiguration.class));

    @Test
    void shouldRegisterMinioClientAndStorageOperatorWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=true",
                        "getboot.storage.type=minio",
                        "getboot.storage.default-bucket=app-default",
                        "getboot.storage.minio.endpoint=http://127.0.0.1:9000",
                        "getboot.storage.minio.access-key=minioadmin",
                        "getboot.storage.minio.secret-key=minioadmin"
                )
                .run(context -> {
                    assertTrue(context.containsBean("storageBucketRouter"));
                    assertTrue(context.containsBean("storageObjectKeyGenerator"));
                    assertTrue(context.containsBean("minioClient"));
                    assertTrue(context.containsBean("storageOperator"));
                    assertNotNull(context.getBean(StorageBucketRouter.class));
                    assertNotNull(context.getBean(StorageObjectKeyGenerator.class));
                    assertNotNull(context.getBean(MinioClient.class));
                    assertTrue(context.getBean(StorageOperator.class) instanceof MinioStorageOperator);
                });
    }

    @Test
    void shouldSkipStorageBeansWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=false",
                        "getboot.storage.minio.endpoint=http://127.0.0.1:9000",
                        "getboot.storage.minio.access-key=minioadmin",
                        "getboot.storage.minio.secret-key=minioadmin"
                )
                .run(context -> {
                    assertFalse(context.containsBean("storageBucketRouter"));
                    assertFalse(context.containsBean("storageObjectKeyGenerator"));
                    assertFalse(context.containsBean("minioClient"));
                    assertFalse(context.containsBean("storageOperator"));
                });
    }

    @Test
    void shouldSkipMinioBeansWhenCredentialsAreMissing() {
        contextRunner
                .withPropertyValues(
                        "getboot.storage.enabled=true",
                        "getboot.storage.default-bucket=app-default"
                )
                .run(context -> {
                    assertTrue(context.containsBean("storageBucketRouter"));
                    assertTrue(context.containsBean("storageObjectKeyGenerator"));
                    assertFalse(context.containsBean("minioClient"));
                    assertFalse(context.containsBean("storageOperator"));
                });
    }
}
