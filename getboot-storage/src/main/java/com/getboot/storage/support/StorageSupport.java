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

import com.getboot.storage.api.exception.StorageException;
import com.getboot.storage.api.model.StoragePresignMethod;
import com.getboot.storage.api.properties.StorageProperties;
import com.getboot.storage.api.request.StorageUploadRequest;
import com.getboot.storage.spi.StorageBucketRouter;
import com.getboot.storage.spi.StorageMetadataCustomizer;
import com.getboot.storage.spi.StorageObjectKeyGenerator;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared storage helper methods.
 *
 * @author qiheng
 */
public final class StorageSupport {

    private StorageSupport() {
    }

    public static String resolveBucket(String scene,
                                       String requestedBucket,
                                       StorageBucketRouter bucketRouter) {
        String bucket = bucketRouter.resolveBucket(scene, requestedBucket);
        if (!StringUtils.hasText(bucket)) {
            throw new StorageException("Storage bucket must not be empty.");
        }
        return bucket;
    }

    public static String resolveObjectKey(String scene,
                                          String requestedObjectKey,
                                          String originalFilename,
                                          StorageObjectKeyGenerator objectKeyGenerator) {
        String objectKey = objectKeyGenerator.generateKey(scene, requestedObjectKey, originalFilename);
        if (!StringUtils.hasText(objectKey)) {
            throw new StorageException("Storage object key must not be empty.");
        }
        return objectKey;
    }

    public static String requireObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new StorageException("Storage object key must not be empty.");
        }
        return objectKey.trim();
    }

    public static Duration resolvePresignTtl(Duration requestedTtl,
                                             StoragePresignMethod method,
                                             StorageProperties properties) {
        Duration ttl = requestedTtl == null ? properties.resolveDefaultTtl(method) : requestedTtl;
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new StorageException("Storage presigned URL ttl must be greater than 0.");
        }
        return ttl;
    }

    public static Map<String, String> mergeMetadata(StorageUploadRequest request,
                                                    List<StorageMetadataCustomizer> metadataCustomizers) {
        Map<String, String> metadata = request.getMetadata() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getMetadata());
        if (metadataCustomizers == null) {
            return metadata;
        }
        metadataCustomizers.forEach(customizer -> customizer.customize(request, metadata));
        return metadata;
    }
}
