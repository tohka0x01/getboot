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
package com.getboot.storage.api.properties;

import com.getboot.storage.api.constant.StorageConstants;
import com.getboot.storage.api.model.StoragePresignMethod;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Storage configuration properties.
 *
 * @author qiheng
 */
@ConfigurationProperties(prefix = "getboot.storage")
public class StorageProperties {

    private boolean enabled = true;

    private String type = StorageConstants.STORAGE_TYPE_MINIO;

    private String defaultBucket;

    private Duration defaultDownloadUrlTtl = StorageConstants.DEFAULT_PRESIGNED_URL_TTL;

    private Duration defaultUploadUrlTtl = StorageConstants.DEFAULT_PRESIGNED_URL_TTL;

    private Map<String, String> sceneBuckets = new LinkedHashMap<>();

    private Minio minio = new Minio();

    public Duration resolveDefaultTtl(StoragePresignMethod method) {
        return method == StoragePresignMethod.UPLOAD ? defaultUploadUrlTtl : defaultDownloadUrlTtl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public Duration getDefaultDownloadUrlTtl() {
        return defaultDownloadUrlTtl;
    }

    public void setDefaultDownloadUrlTtl(Duration defaultDownloadUrlTtl) {
        this.defaultDownloadUrlTtl = defaultDownloadUrlTtl;
    }

    public Duration getDefaultUploadUrlTtl() {
        return defaultUploadUrlTtl;
    }

    public void setDefaultUploadUrlTtl(Duration defaultUploadUrlTtl) {
        this.defaultUploadUrlTtl = defaultUploadUrlTtl;
    }

    public Map<String, String> getSceneBuckets() {
        return sceneBuckets;
    }

    public void setSceneBuckets(Map<String, String> sceneBuckets) {
        this.sceneBuckets = sceneBuckets == null ? new LinkedHashMap<>() : new LinkedHashMap<>(sceneBuckets);
    }

    public Minio getMinio() {
        return minio;
    }

    public void setMinio(Minio minio) {
        this.minio = minio;
    }

    public static class Minio {

        private boolean enabled = true;

        private String endpoint;

        private String publicEndpoint;

        private String accessKey;

        private String secretKey;

        private String region;

        private boolean createBucketIfMissing = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getPublicEndpoint() {
            return publicEndpoint;
        }

        public void setPublicEndpoint(String publicEndpoint) {
            this.publicEndpoint = publicEndpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public boolean isCreateBucketIfMissing() {
            return createBucketIfMissing;
        }

        public void setCreateBucketIfMissing(boolean createBucketIfMissing) {
            this.createBucketIfMissing = createBucketIfMissing;
        }
    }
}
