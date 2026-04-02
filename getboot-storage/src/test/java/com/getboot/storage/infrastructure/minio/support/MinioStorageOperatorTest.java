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
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MinioStorageOperatorTest {

    @Test
    void shouldUploadObjectAndCreateBucketWhenMissing() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> "invoice/2026/04/02/file.pdf",
                List.of((request, metadata) -> metadata.put("traceid", "trace-001"))
        );
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(
                new ObjectWriteResponse(
                        emptyHeaders(),
                        "invoice-bucket",
                        "us-east-1",
                        "invoice/2026/04/02/file.pdf",
                        "etag-001",
                        "version-001"
                )
        );

        StorageUploadRequest request = new StorageUploadRequest();
        request.setScene("invoice");
        request.setOriginalFilename("Receipt.PDF");
        request.setContentType("application/pdf");
        request.setContentLength(3L);
        request.setInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        request.setMetadata(Map.of("tenant", "acme"));

        StorageObjectMetadata metadata = operator.upload(request);

        ArgumentCaptor<BucketExistsArgs> bucketExistsArgs = ArgumentCaptor.forClass(BucketExistsArgs.class);
        ArgumentCaptor<MakeBucketArgs> makeBucketArgs = ArgumentCaptor.forClass(MakeBucketArgs.class);
        ArgumentCaptor<PutObjectArgs> putObjectArgs = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).bucketExists(bucketExistsArgs.capture());
        verify(minioClient).makeBucket(makeBucketArgs.capture());
        verify(minioClient).putObject(putObjectArgs.capture());

        assertEquals("invoice-bucket", bucketExistsArgs.getValue().bucket());
        assertEquals("invoice-bucket", makeBucketArgs.getValue().bucket());
        assertEquals("us-east-1", makeBucketArgs.getValue().region());
        assertEquals("invoice-bucket", putObjectArgs.getValue().bucket());
        assertEquals("invoice/2026/04/02/file.pdf", putObjectArgs.getValue().object());
        assertEquals("application/pdf", putObjectArgs.getValue().contentType());

        assertEquals("invoice-bucket", metadata.getBucket());
        assertEquals("invoice/2026/04/02/file.pdf", metadata.getObjectKey());
        assertEquals(3L, metadata.getContentLength());
        assertEquals("application/pdf", metadata.getContentType());
        assertEquals("etag-001", metadata.getEtag());
        assertEquals("version-001", metadata.getVersionId());
        assertEquals(Map.of("tenant", "acme", "traceid", "trace-001"), metadata.getMetadata());
    }

    @Test
    void shouldMapDownloadMetadataAndPayload() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> requestedObjectKey,
                List.of()
        );
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(
                statObjectResponse(
                        "invoice-bucket",
                        "invoice/ready.pdf",
                        7L,
                        "application/pdf",
                        "etag-002",
                        "version-002",
                        Map.of("tenant", "acme")
                )
        );
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(
                new GetObjectResponse(
                        emptyHeaders(),
                        "invoice-bucket",
                        "us-east-1",
                        "invoice/ready.pdf",
                        new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8))
                )
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

        ArgumentCaptor<StatObjectArgs> statObjectArgs = ArgumentCaptor.forClass(StatObjectArgs.class);
        ArgumentCaptor<GetObjectArgs> getObjectArgs = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).statObject(statObjectArgs.capture());
        verify(minioClient).getObject(getObjectArgs.capture());
        assertEquals("invoice-bucket", statObjectArgs.getValue().bucket());
        assertEquals("invoice/ready.pdf", statObjectArgs.getValue().object());
        assertEquals("invoice-bucket", getObjectArgs.getValue().bucket());
        assertEquals("invoice/ready.pdf", getObjectArgs.getValue().object());
    }

    @Test
    void shouldRemoveObjectFromResolvedBucket() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
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

        ArgumentCaptor<RemoveObjectArgs> removeObjectArgs = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(removeObjectArgs.capture());
        assertEquals("invoice-bucket", removeObjectArgs.getValue().bucket());
        assertEquals("invoice/obsolete.pdf", removeObjectArgs.getValue().object());
    }

    @Test
    void shouldGenerateUploadPresignedUrlWithPublicEndpoint() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        StorageProperties properties = storageProperties();
        MinioStorageOperator operator = newOperator(
                minioClient,
                properties,
                (scene, requestedObjectKey, originalFilename) -> "invoice/2026/04/02/upload.png",
                List.of()
        );
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(
                "http://minio-internal:9000/invoice-bucket/invoice/2026/04/02/upload.png?X-Amz-Signature=abc"
        );

        StoragePresignRequest request = new StoragePresignRequest();
        request.setMethod(StoragePresignMethod.UPLOAD);
        request.setScene("invoice");
        request.setOriginalFilename("upload.png");
        request.setTtl(Duration.ofMinutes(5));

        StoragePresignResponse response = operator.generatePresignedUrl(request);

        ArgumentCaptor<GetPresignedObjectUrlArgs> presignArgs = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).getPresignedObjectUrl(presignArgs.capture());
        assertEquals("invoice-bucket", presignArgs.getValue().bucket());
        assertEquals("invoice/2026/04/02/upload.png", presignArgs.getValue().object());
        assertEquals(Method.PUT, presignArgs.getValue().method());
        assertEquals(300, presignArgs.getValue().expiry());

        assertEquals("invoice-bucket", response.getBucket());
        assertEquals("invoice/2026/04/02/upload.png", response.getObjectKey());
        assertEquals(StoragePresignMethod.UPLOAD, response.getMethod());
        assertEquals(Duration.ofMinutes(5), response.getTtl());
        assertEquals(
                "https://files.example.com/invoice-bucket/invoice/2026/04/02/upload.png?X-Amz-Signature=abc",
                response.getUrl()
        );
    }

    @Test
    void shouldGenerateDownloadPresignedUrlWithDefaultTtl() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioStorageOperator operator = newOperator(
                minioClient,
                storageProperties(),
                (scene, requestedObjectKey, originalFilename) -> requestedObjectKey,
                List.of()
        );
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(
                "http://minio-internal:9000/invoice-bucket/invoice/ready.pdf?X-Amz-Signature=download"
        );

        StoragePresignRequest request = new StoragePresignRequest();
        request.setMethod(StoragePresignMethod.DOWNLOAD);
        request.setScene("invoice");
        request.setObjectKey("invoice/ready.pdf");

        StoragePresignResponse response = operator.generatePresignedUrl(request);

        ArgumentCaptor<GetPresignedObjectUrlArgs> presignArgs = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(presignArgs.capture());
        verifyNoMoreInteractions(minioClient);
        assertEquals("invoice-bucket", presignArgs.getValue().bucket());
        assertEquals("invoice/ready.pdf", presignArgs.getValue().object());
        assertEquals(Method.GET, presignArgs.getValue().method());
        assertEquals(900, presignArgs.getValue().expiry());

        assertEquals(StoragePresignMethod.DOWNLOAD, response.getMethod());
        assertEquals(Duration.ofMinutes(15), response.getTtl());
        assertEquals(
                "https://files.example.com/invoice-bucket/invoice/ready.pdf?X-Amz-Signature=download",
                response.getUrl()
        );
    }

    private MinioStorageOperator newOperator(MinioClient minioClient,
                                             StorageProperties properties,
                                             StorageObjectKeyGenerator objectKeyGenerator,
                                             List<StorageMetadataCustomizer> customizers) {
        StorageBucketRouter bucketRouter = new DefaultStorageBucketRouter(properties);
        return new MinioStorageOperator(minioClient, bucketRouter, objectKeyGenerator, customizers, properties);
    }

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

    private Headers emptyHeaders() {
        return new Headers.Builder().build();
    }

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
}
