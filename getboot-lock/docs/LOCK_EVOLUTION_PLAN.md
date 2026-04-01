# getboot-lock 演进规划

这份文档只记录 `getboot-lock` 下一阶段还要继续推进的内容。已经落地的能力不再作为 TODO 反复保留。

## 当前状态

`getboot-lock` 已经提供以下稳定能力：

- 统一注解入口 `@DistributedLock`
- 统一扩展点 `DistributedLockKeyResolver`
- 统一扩展点 `DistributedLockAcquireFailureHandler`
- `redis.redisson` 实现
- `database.jdbc` 实现

共享行为也已经收敛完成：

- 完整锁键统一为 `<key-prefix>:<scene>#<resolved-key>`
- `getboot.lock.type` 用于切换具体实现
- 获取锁失败时统一经过 `DistributedLockAcquireFailureHandler`
- 如果失败处理器返回正常，框架仍会抛出 `DistributedLockException`，避免无锁继续执行业务

## 剩余目标

下一阶段只保留一个明确目标：

- 增加 `infrastructure.zookeeper.curator.*`

## 继续扩展时的边界

后续新增实现时，继续保持下面这些边界不变：

- 业务入口仍然只有 `@DistributedLock`
- `scene`、`key`、`keyExpression`、`waitTime`、`expireTime` 的语义保持一致
- 不把 Redis、JDBC、ZooKeeper 之类的技术细节上浮到 `api.*`
- 技术栈绑定实现统一放在 `infrastructure.*`

目标结构：

```text
com.getboot.lock
├── api
├── spi
├── support
└── infrastructure
    ├── redis.redisson
    ├── database.jdbc
    └── zookeeper.curator
```

## 配置基线

后续继续沿用当前配置模型：

```yaml
getboot:
  lock:
    enabled: true
    type: redis
    redis:
      enabled: true
      key-prefix: distributed_lock
    database:
      enabled: false
      key-prefix: distributed_lock
      table-name: distributed_lock
      lease-ms: 30000
      retry-interval-ms: 100
      initialize-schema: false
    zookeeper:
      enabled: false
      key-prefix: distributed_lock
      base-path: /getboot/lock
```

其中：

- `enabled` 和 `type` 继续保留在 `getboot.lock.*`
- 各实现使用各自子树配置
- 底层基础设施专有配置仍由对应基础模块承接，不把业务入口直接做成第三方原生配置镜像

## 下一步建议

建议按这个顺序继续：

1. 增加 `zookeeper.curator` 子树
2. 为 ZooKeeper 实现补充和 Redis / JDBC 一致的测试覆盖
3. 更新 README、示例配置和根 `docs/TODO.md`

## 完成标准

可以认为下一阶段完成，至少满足：

- `getboot-lock` 下新增 `zookeeper.curator` 实现子树
- `@DistributedLock` 的业务使用方式不需要调整
- 三种实现共享同一套锁键规则和失败处理语义
- README 能准确说明默认实现、已支持实现和各自前置条件
