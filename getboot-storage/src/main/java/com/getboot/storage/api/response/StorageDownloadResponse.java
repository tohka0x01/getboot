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
package com.getboot.storage.api.response;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Object download response.
 *
 * @author qiheng
 */
public class StorageDownloadResponse implements AutoCloseable {

    private final String bucket;
    private final String objectKey;
    private final long contentLength;
    private final String contentType;
    private final String etag;
    private final String versionId;
    private final Map<String, String> metadata;
    private final InputStream inputStream;

    public StorageDownloadResponse(String bucket,
                                   String objectKey,
                                   long contentLength,
                                   String contentType,
                                   String etag,
                                   String versionId,
                                   Map<String, String> metadata,
                                   InputStream inputStream) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.etag = etag;
        this.versionId = versionId;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        this.inputStream = inputStream;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public String getVersionId() {
        return versionId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
