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
package com.getboot.storage.support;

import com.getboot.storage.api.properties.StorageProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DefaultStorageBucketRouter} 测试。
 *
 * @author qiheng
 */
class DefaultStorageBucketRouterTest {

    /**
     * 验证显式 bucket、场景 bucket 与默认 bucket 的优先级。
     */
    @Test
    void shouldResolveBucketByRequestedBucketSceneAndDefaultOrder() {
        StorageProperties properties = new StorageProperties();
        properties.setDefaultBucket(" default-bucket ");
        properties.setSceneBuckets(Map.of("invoice", " invoice-bucket "));
        DefaultStorageBucketRouter bucketRouter = new DefaultStorageBucketRouter(properties);

        assertEquals("requested-bucket", bucketRouter.resolveBucket("invoice", " requested-bucket "));
        assertEquals("invoice-bucket", bucketRouter.resolveBucket(" invoice ", null));
        assertEquals("default-bucket", bucketRouter.resolveBucket("unknown", null));
    }

    /**
     * 验证所有 bucket 信息缺失时返回空字符串。
     */
    @Test
    void shouldReturnEmptyBucketWhenNoBucketConfigured() {
        DefaultStorageBucketRouter bucketRouter = new DefaultStorageBucketRouter(new StorageProperties());

        assertEquals("", bucketRouter.resolveBucket("unknown", null));
    }
}
