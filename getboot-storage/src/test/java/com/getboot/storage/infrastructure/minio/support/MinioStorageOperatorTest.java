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
package com.getboot.storage.infrastructure.minio.support;

import com.getboot.storage.api.model.StorageObjectMetadata;
import com.getboot.storage.api.model.StoragePresignMethod;
import com.getboot.storage.api.properties.StorageProperties;
import com.getboot.storage.api.request.StorageObjectRequest;
import com.getboot.storage.api.request.StoragePresignRequest;
import com.getboot.storage.api.request.StorageUploadRequest;
import com.getboot.storage.api.response.StorageDownloadResponse;
import com.getboot.storage.api.response.StoragePresignResponse;
import com.getboot.storage.spi.StorageBucketRouter;
import com.getboot.storage.spi.StorageMetadataCustomizer;
import com.getboot.storage.spi.StorageObjectKeyGenerator;
import com.getboot.storage.support.DefaultStorageBucketRouter;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MinIO 对象存储门面测试。
 *
 * @author qiheng
 */
class MinioStorageOperatorTest {

    /**
     * 验证上传对象时自动创建存储桶并返回元数据。
     *
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    void shouldUploadObjectAndCreateBucketWhenMissing() throws Exception {
        RecordingMinioClient minioClient = new RecordingMinioClient();
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> "invoice/2026/04/02/file.pdf",
                List.of((request, metadata) -> metadata.put("traceid", "trace-001"))
        );
        minioClient.bucketExistsResult = false;
        minioClient.putObjectResponse = new ObjectWriteResponse(
                emptyHeaders(),
                "invoice-bucket",
                "us-east-1",
                "invoice/2026/04/02/file.pdf",
                "etag-001",
                "version-001"
        );

        StorageUploadRequest request = new StorageUploadRequest();
        request.setScene("invoice");
        request.setOriginalFilename("Receipt.PDF");
        request.setContentType("application/pdf");
        request.setContentLength(3L);
        request.setInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        request.setMetadata(Map.of("tenant", "acme"));

        StorageObjectMetadata metadata = operator.upload(request);

        assertNotNull(minioClient.bucketExistsArgs);
        assertNotNull(minioClient.makeBucketArgs);
        assertNotNull(minioClient.putObjectArgs);
        assertEquals("invoice-bucket", minioClient.bucketExistsArgs.bucket());
        assertEquals("invoice-bucket", minioClient.makeBucketArgs.bucket());
        assertEquals("us-east-1", minioClient.makeBucketArgs.region());
        assertEquals("invoice-bucket", minioClient.putObjectArgs.bucket());
        assertEquals("invoice/2026/04/02/file.pdf", minioClient.putObjectArgs.object());
        assertEquals("application/pdf", minioClient.putObjectArgs.contentType());

        assertEquals("invoice-bucket", metadata.getBucket());
        assertEquals("invoice/2026/04/02/file.pdf", metadata.getObjectKey());
        assertEquals(3L, metadata.getContentLength());
        assertEquals("application/pdf", metadata.getContentType());
        assertEquals("etag-001", metadata.getEtag());
        assertEquals("version-001", metadata.getVersionId());
        assertEquals(Map.of("tenant", "acme", "traceid", "trace-001"), metadata.getMetadata());
    }

    /**
     * 验证下载对象时映射元数据与文件内容。
     *
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    void shouldMapDownloadMetadataAndPayload() throws Exception {
        RecordingMinioClient minioClient = new RecordingMinioClient();
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> requestedObjectKey,
                List.of()
        );
        minioClient.statObjectResponse = statObjectResponse(
                "invoice-bucket",
                "invoice/ready.pdf",
                7L,
                "application/pdf",
                "etag-002",
                "version-002",
                Map.of("tenant", "acme")
        );
        minioClient.getObjectResponse = new GetObjectResponse(
                emptyHeaders(),
                "invoice-bucket",
                "us-east-1",
                "invoice/ready.pdf",
                new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8))
        );

        StorageObjectRequest request = new StorageObjectRequest();
        request.setScene("invoice");
        request.setObjectKey("invoice/ready.pdf");

        try (StorageDownloadResponse response = operator.download(request)) {
            assertEquals("invoice-bucket", response.getBucket());
            assertEquals("invoice/ready.pdf", response.getObjectKey());
            assertEquals(7L, response.getContentLength());
            assertEquals("application/pdf", response.getContentType());
            assertEquals("etag-002", response.getEtag());
            assertEquals("version-002", response.getVersionId());
            assertEquals(Map.of("tenant", "acme"), response.getMetadata());
            assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), response.getInputStream().readAllBytes());
        }

        assertNotNull(minioClient.statObjectArgs);
        assertNotNull(minioClient.getObjectArgs);
        assertEquals("invoice-bucket", minioClient.statObjectArgs.bucket());
        assertEquals("invoice/ready.pdf", minioClient.statObjectArgs.object());
        assertEquals("invoice-bucket", minioClient.getObjectArgs.bucket());
        assertEquals("invoice/ready.pdf", minioClient.getObjectArgs.object());
    }

    /**
     * 验证删除对象时使用解析后的存储桶。
     *
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    void shouldRemoveObjectFromResolvedBucket() throws Exception {
        RecordingMinioClient minioClient = new RecordingMinioClient();
        MinioStorageOperator operator = newOperator(
                minioClient,
                storageProperties(),
                (scene, requestedObjectKey, originalFilename) -> requestedObjectKey,
                List.of()
        );

        StorageObjectRequest request = new StorageObjectRequest();
        request.setScene("invoice");
        request.setObjectKey("invoice/obsolete.pdf");
        operator.delete(request);

        assertNotNull(minioClient.removeObjectArgs);
        assertEquals("invoice-bucket", minioClient.removeObjectArgs.bucket());
        assertEquals("invoice/obsolete.pdf", minioClient.removeObjectArgs.object());
    }

    /**
     * 验证上传预签名地址使用公共域名。
     *
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    void shouldGenerateUploadPresignedUrlWithPublicEndpoint() throws Exception {
        RecordingMinioClient minioClient = new RecordingMinioClient();
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> "invoice/2026/04/02/upload.png",
                List.of()
        );
        minioClient.bucketExistsResult = true;
        minioClient.presignedObjectUrl =
                "http://minio-internal:9000/invoice-bucket/invoice/2026/04/02/upload.png?X-Amz-Signature=abc";

        StoragePresignRequest request = new StoragePresignRequest();
        request.setMethod(StoragePresignMethod.UPLOAD);
        request.setScene("invoice");
        request.setOriginalFilename("upload.png");
        request.setTtl(Duration.ofMinutes(5));

        StoragePresignResponse response = operator.generatePresignedUrl(request);

        assertNotNull(minioClient.bucketExistsArgs);
        assertNotNull(minioClient.presignedObjectUrlArgs);
        assertEquals("invoice-bucket", minioClient.presignedObjectUrlArgs.bucket());
        assertEquals("invoice/2026/04/02/upload.png", minioClient.presignedObjectUrlArgs.object());
        assertEquals(Method.PUT, minioClient.presignedObjectUrlArgs.method());
        assertEquals(300, minioClient.presignedObjectUrlArgs.expiry());

        assertEquals("invoice-bucket", response.getBucket());
        assertEquals("invoice/2026/04/02/upload.png", response.getObjectKey());
        assertEquals(StoragePresignMethod.UPLOAD, response.getMethod());
        assertEquals(Duration.ofMinutes(5), response.getTtl());
        assertEquals(
                "https://files.example.com/invoice-bucket/invoice/2026/04/02/upload.png?X-Amz-Signature=abc",
                response.getUrl()
        );
    }

    /**
     * 验证下载预签名地址使用默认有效期。
     *
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    void shouldGenerateDownloadPresignedUrlWithDefaultTtl() throws Exception {
        RecordingMinioClient minioClient = new RecordingMinioClient();
        MinioStorageOperator operator = newOperator(
                minioClient,
                storageProperties(),
                (scene, requestedObjectKey, originalFilename) -> requestedObjectKey,
                List.of()
        );
        minioClient.presignedObjectUrl =
                "http://minio-internal:9000/invoice-bucket/invoice/ready.pdf?X-Amz-Signature=download";

        StoragePresignRequest request = new StoragePresignRequest();
        request.setMethod(StoragePresignMethod.DOWNLOAD);
        request.setScene("invoice");
        request.setObjectKey("invoice/ready.pdf");

        StoragePresignResponse response = operator.generatePresignedUrl(request);

        assertNotNull(minioClient.presignedObjectUrlArgs);
        assertEquals("invoice-bucket", minioClient.presignedObjectUrlArgs.bucket());
        assertEquals("invoice/ready.pdf", minioClient.presignedObjectUrlArgs.object());
        assertEquals(Method.GET, minioClient.presignedObjectUrlArgs.method());
        assertEquals(900, minioClient.presignedObjectUrlArgs.expiry());
        assertNull(minioClient.bucketExistsArgs);

        assertEquals(StoragePresignMethod.DOWNLOAD, response.getMethod());
        assertEquals(Duration.ofMinutes(15), response.getTtl());
        assertEquals(
                "https://files.example.com/invoice-bucket/invoice/ready.pdf?X-Amz-Signature=download",
                response.getUrl()
        );
    }

    /**
     * 构造 MinIO 对象存储门面。
     *
     * @param minioClient MinIO 客户端
     * @param properties 对象存储配置
     * @param objectKeyGenerator 对象键生成器
     * @param customizers 元数据定制器集合
     * @return MinIO 对象存储门面
     */
    private MinioStorageOperator newOperator(MinioClient minioClient,
                                             StorageProperties properties,
                                             StorageObjectKeyGenerator objectKeyGenerator,
                                             List<StorageMetadataCustomizer> customizers) {
        StorageBucketRouter bucketRouter = new DefaultStorageBucketRouter(properties);
        return new MinioStorageOperator(minioClient, bucketRouter, objectKeyGenerator, customizers, properties);
    }

    /**
     * 构造测试用对象存储配置。
     *
     * @return 对象存储配置
     */
    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.setDefaultBucket("default-bucket");
        properties.setSceneBuckets(Map.of("invoice", "invoice-bucket"));
        properties.getMinio().setEndpoint("http://minio-internal:9000");
        properties.getMinio().setPublicEndpoint("https://files.example.com");
        properties.getMinio().setRegion("us-east-1");
        properties.getMinio().setCreateBucketIfMissing(true);
        return properties;
    }

    /**
     * 构造空响应头。
     *
     * @return 空响应头
     */
    private Headers emptyHeaders() {
        return new Headers.Builder().build();
    }

    /**
     * 构造对象元数据响应。
     *
     * @param bucket 存储桶
     * @param objectKey 对象键
     * @param contentLength 内容长度
     * @param contentType 内容类型
     * @param etag ETag
     * @param versionId 版本号
     * @param metadata 用户元数据
     * @return MinIO 元数据响应
     */
    private StatObjectResponse statObjectResponse(String bucket,
                                                  String objectKey,
                                                  long contentLength,
                                                  String contentType,
                                                  String etag,
                                                  String versionId,
                                                  Map<String, String> metadata) {
        Headers.Builder builder = new Headers.Builder()
                .add("ETag", "\"" + etag + "\"")
                .add("Content-Length", String.valueOf(contentLength))
                .add("Last-Modified", DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                        .format(ZonedDateTime.now(java.time.ZoneOffset.UTC)))
                .add("Content-Type", contentType)
                .add("x-amz-version-id", versionId);
        metadata.forEach((key, value) -> builder.add("x-amz-meta-" + key, value));
        return new StatObjectResponse(builder.build(), bucket, "us-east-1", objectKey);
    }

    /**
     * 记录参数的测试用 MinIO 客户端。
     */
    private static final class RecordingMinioClient extends MinioClient {

        /**
         * bucket 是否存在。
         */
        private boolean bucketExistsResult = true;

        /**
         * 记录的 bucketExists 参数。
         */
        private BucketExistsArgs bucketExistsArgs;

        /**
         * 记录的 makeBucket 参数。
         */
        private MakeBucketArgs makeBucketArgs;

        /**
         * 记录的 putObject 参数。
         */
        private PutObjectArgs putObjectArgs;

        /**
         * 记录的 statObject 参数。
         */
        private StatObjectArgs statObjectArgs;

        /**
         * 记录的 getObject 参数。
         */
        private GetObjectArgs getObjectArgs;

        /**
         * 记录的 removeObject 参数。
         */
        private RemoveObjectArgs removeObjectArgs;

        /**
         * 记录的预签名参数。
         */
        private GetPresignedObjectUrlArgs presignedObjectUrlArgs;

        /**
         * 预设上传响应。
         */
        private ObjectWriteResponse putObjectResponse;

        /**
         * 预设查询响应。
         */
        private StatObjectResponse statObjectResponse;

        /**
         * 预设下载响应。
         */
        private GetObjectResponse getObjectResponse;

        /**
         * 预设预签名地址。
         */
        private String presignedObjectUrl;

        /**
         * 创建测试客户端。
         */
        private RecordingMinioClient() {
            super(MinioClient.builder()
                    .endpoint("http://127.0.0.1:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build());
        }

        /**
         * 记录 bucket 存在性检查参数。
         *
         * @param args bucket 检查参数
         * @return 预设存在性结果
         */
        @Override
        public boolean bucketExists(BucketExistsArgs args) {
            this.bucketExistsArgs = args;
            return bucketExistsResult;
        }

        /**
         * 记录创建 bucket 参数。
         *
         * @param args 创建参数
         */
        @Override
        public void makeBucket(MakeBucketArgs args) {
            this.makeBucketArgs = args;
        }

        /**
         * 记录上传参数并返回预设响应。
         *
         * @param args 上传参数
         * @return 预设上传响应
         */
        @Override
        public ObjectWriteResponse putObject(PutObjectArgs args) {
            this.putObjectArgs = args;
            return putObjectResponse;
        }

        /**
         * 记录查询参数并返回预设响应。
         *
         * @param args 查询参数
         * @return 预设查询响应
         */
        @Override
        public StatObjectResponse statObject(StatObjectArgs args) {
            this.statObjectArgs = args;
            return statObjectResponse;
        }

        /**
         * 记录下载参数并返回预设响应。
         *
         * @param args 下载参数
         * @return 预设下载响应
         */
        @Override
        public GetObjectResponse getObject(GetObjectArgs args) {
            this.getObjectArgs = args;
            return getObjectResponse;
        }

        /**
         * 记录删除参数。
         *
         * @param args 删除参数
         */
        @Override
        public void removeObject(RemoveObjectArgs args) {
            this.removeObjectArgs = args;
        }

        /**
         * 记录预签名参数并返回预设地址。
         *
         * @param args 预签名参数
         * @return 预设预签名地址
         */
        @Override
        public String getPresignedObjectUrl(GetPresignedObjectUrlArgs args) {
            this.presignedObjectUrlArgs = args;
            return presignedObjectUrl;
        }

        /**
         * 测试中不需要真正关闭资源。
         *
         * @throws IOException 不会抛出
         */
        @Override
        public void close() throws IOException {
        }
    }
}
