# getboot-storage

对象存储 starter，当前第一版先把最常见、最容易散落到业务代码里的那层能力收口：

- 统一提供 `StorageOperator` 作为上传、下载、删除、查询元数据和预签名 URL 门面
- 统一管理 bucket 路由、对象 key 生成和上传元数据增强
- 先落地 `MinIO` 实现，避免业务层直接耦合 SDK

## 作用

适合这几类场景：

- 业务里需要上传附件、头像、发票、回单、导出文件这类对象
- 想把预签名上传 / 下载 URL 的生成逻辑从控制器和工具类里抽出来
- 想统一 bucket 路由、对象 key 命名和元数据写入规则，而不是每个项目各写一套

## 接入方式

业务项目继承父 `pom` 后，引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-storage</artifactId>
</dependency>
```

当前第一版只内置 `MinIO` 实现，配置好 MinIO 连接信息后即可直接使用。

## 前置条件

- 当前没有强制配套 `getboot-*` 模块
- 需要准备可访问的对象存储服务，第一版按 `MinIO` 配置接入

## 目录约定

- `api.*`
  对外稳定能力入口、请求模型、响应模型和配置
- `spi`
  bucket 路由、对象 key 生成、上传元数据增强扩展点
- `support`
  默认 bucket 路由、默认对象 key 规则和统一辅助方法
- `infrastructure.minio.*`
  MinIO SDK 适配和自动装配

## 配置示例

```yaml
getboot:
  storage:
    enabled: true
    type: minio
    default-bucket: app-default
    default-download-url-ttl: 15m
    default-upload-url-ttl: 15m
    scene-buckets:
      invoice: billing-bucket
      avatar: user-assets
    minio:
      enabled: true
      endpoint: http://127.0.0.1:9000
      public-endpoint: https://files.example.com
      access-key: minioadmin
      secret-key: minioadmin
      region: us-east-1
      create-bucket-if-missing: true
```

- `default-bucket`
  没有显式指定 bucket，且 `scene-buckets` 也没命中时的默认 bucket
- `scene-buckets`
  按业务场景路由 bucket，例如发票、头像、合同走不同 bucket
- `default-download-url-ttl` / `default-upload-url-ttl`
  预签名 URL 默认有效期
- `minio.public-endpoint`
  生成预签名 URL 后，对外暴露域名与内网 MinIO 地址不一致时用于改写返回 URL
- `minio.create-bucket-if-missing`
  上传和上传预签名场景下，是否自动补建 bucket

## 使用示例

```java
@Service
public class FileApplicationService {

    private final StorageOperator storageOperator;

    public FileApplicationService(StorageOperator storageOperator) {
        this.storageOperator = storageOperator;
    }

    public StorageObjectMetadata uploadAvatar(InputStream inputStream, long contentLength) {
        StorageUploadRequest request = new StorageUploadRequest();
        request.setScene("avatar");
        request.setOriginalFilename("avatar.png");
        request.setContentType("image/png");
        request.setContentLength(contentLength);
        request.setInputStream(inputStream);
        request.getMetadata().put("tenant", "demo");
        return storageOperator.upload(request);
    }

    public StoragePresignResponse createDownloadUrl(String objectKey) {
        StoragePresignRequest request = new StoragePresignRequest();
        request.setMethod(StoragePresignMethod.DOWNLOAD);
        request.setScene("avatar");
        request.setObjectKey(objectKey);
        return storageOperator.generatePresignedUrl(request);
    }

    public byte[] download(String objectKey) throws IOException {
        StorageObjectRequest request = new StorageObjectRequest();
        request.setScene("avatar");
        request.setObjectKey(objectKey);
        try (StorageDownloadResponse response = storageOperator.download(request)) {
            return response.getInputStream().readAllBytes();
        }
    }
}
```

`StorageDownloadResponse` 持有底层输入流，下载完成后要记得关闭，推荐直接用 `try-with-resources`。

## 默认 Bean

- `StorageBucketRouter`
  默认实现为 `DefaultStorageBucketRouter`
- `StorageObjectKeyGenerator`
  默认实现为 `DefaultStorageObjectKeyGenerator`
- `MinioClient`
  当 `getboot.storage.minio.endpoint/access-key/secret-key` 存在时自动注册
- `StorageOperator`
  当容器内存在 `MinioClient` 时，默认实现为 `MinioStorageOperator`

## 扩展点

- 可注册 `StorageBucketRouter` Bean，自定义 bucket 路由规则
- 可注册 `StorageObjectKeyGenerator` Bean，自定义对象 key 命名规则
- 可注册一个或多个 `StorageMetadataCustomizer` Bean，在上传前补充元数据

## 已实现技术栈

- `MinIO`

## 边界 / 补充文档

- 当前第一版只承接对象上传、下载、删除、元数据读取和预签名 URL，不覆盖分片上传编排、媒体转码、审核、CDN 域名治理
- 当前只内置 `MinIO` 实现，`OSS` / `S3` 等提供商后续再补
- 配置示例可直接参考 [`getboot-storage.yml.example`](./src/main/resources/getboot-storage.yml.example)
