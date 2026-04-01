# getboot-lock

声明式分布式锁 starter，当前支持 `redis.redisson` 与 `database.jdbc` 两种实现。

## 作用

- 提供统一注解入口 `@DistributedLock`
- 提供统一锁键解析 SPI `DistributedLockKeyResolver`
- 提供统一失败处理 SPI `DistributedLockAcquireFailureHandler`
- 提供 Redis / Redisson 与 JDBC 两套切面实现

## 接入方式

业务项目继承父 `pom` 后，引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-lock</artifactId>
</dependency>
```

## 当前支持实现

| `getboot.lock.type` | 实现 | 前置条件 | 说明 |
| --- | --- | --- | --- |
| `redis` | `infrastructure.redis.redisson.*` | Spring 容器中存在 `RedissonClient` | 默认实现 |
| `database` | `infrastructure.database.jdbc.*` | Spring 容器中存在单个 `DataSource` | 适合没有 Redis 锁基础设施的场景 |

## 配置说明

通用开关与选择器：

```yaml
getboot:
  lock:
    enabled: true
    type: redis
```

Redis / Redisson：

```yaml
getboot:
  lock:
    enabled: true
    type: redis
    redis:
      enabled: true
      key-prefix: distributed_lock
  coordination:
    redisson:
      file: classpath:redisson/redisson.yaml
```

JDBC 数据库锁：

```yaml
getboot:
  lock:
    enabled: true
    type: database
    database:
      enabled: true
      key-prefix: distributed_lock
      table-name: distributed_lock
      lease-ms: 30000
      retry-interval-ms: 100
      initialize-schema: false
```

`database.initialize-schema=true` 只适合本地或简单环境快速启动。生产环境建议自行管理建表 SQL，并参考 [`getboot-lock-database.sql.example`](./src/main/resources/getboot-lock-database.sql.example)。

## 锁语义

- 完整锁键格式统一为 `<key-prefix>:<scene>#<resolved-key>`
- `key` 优先于 `keyExpression`
- `waitTime=Integer.MAX_VALUE` 表示一直等待
- `expireTime=-1` 会回退到具体实现的默认租约策略
- 获取锁失败时会先调用 `DistributedLockAcquireFailureHandler`
- 如果自定义失败处理器没有抛异常，框架仍会抛出 `DistributedLockException`，业务方法不会继续执行

## 默认 Bean

- `DistributedLockKeyResolver` 默认实现为 `SpelDistributedLockKeyResolver`
- `DistributedLockAcquireFailureHandler` 默认实现为 `DefaultDistributedLockAcquireFailureHandler`
- `DistributedLockAspect` 在 `type=redis` 时注册
- `JdbcDistributedLockAspect` 在 `type=database` 时注册

## 目录约定

- `api.*`：注解、常量、异常、配置模型
- `spi.*`：锁键解析与失败处理扩展点
- `support.*`：共享锁键拼装、等待时间与失败处理辅助
- `infrastructure.redis.redisson.*`：Redis / Redisson 实现
- `infrastructure.database.jdbc.*`：JDBC 数据库锁实现

## 已实现技术栈

- Redis Lock
- Redisson
- JDBC Database Lock

## 补充文档

- 配置示例：[`getboot-lock.yml.example`](./src/main/resources/getboot-lock.yml.example)
- 建表示例：[`getboot-lock-database.sql.example`](./src/main/resources/getboot-lock-database.sql.example)
- 后续规划：[`docs/LOCK_EVOLUTION_PLAN.md`](./docs/LOCK_EVOLUTION_PLAN.md)
