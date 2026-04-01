# getboot-lock 扩展规划

这份文档服务于 `docs/TODO.md` 里 `getboot-lock` 的下一阶段任务：

- 数据库分布式锁实现子树
- ZooKeeper 分布式锁实现子树
- 不同实现之间的默认失败策略、Key 解析策略与扩展点统一

它不是当前接入文档。当前模块怎么引、怎么配、现在已经提供什么能力，先看 [`../README.md`](../README.md)；这份文档只回答“下一步准备怎么扩”。

## 1. 当前状态

当前 `getboot-lock` 已经稳定对外暴露的能力层只有三部分：

- `@DistributedLock`
  业务方法的统一声明式入口
- `DistributedLockKeyResolver`
  业务锁键解析 SPI
- `DistributedLockAcquireFailureHandler`
  锁获取失败处理 SPI

当前唯一实现是：

- `infrastructure.redis.redisson.*`

当前默认行为是：

- 锁全键格式：`<key-prefix>:<scene>#<resolved-key>`
- 锁键解析：优先 `key`，否则走 `keyExpression`
- 获取失败：默认抛出 `DistributedLockException`

这三点应继续保留为后续实现的共同基线，不因为新增数据库锁或 ZooKeeper 锁而推翻外部接口。

## 2. 扩展目标

下一阶段 `getboot-lock` 的目标不是把不同锁实现揉成一团，而是保持：

- 对外仍然只有一个能力模块：`getboot-lock`
- 业务方法仍然统一通过 `@DistributedLock` 使用
- 业务方继续优先扩展 `spi`，而不是直接依赖某种实现
- 不同实现的差异尽量收敛在 `infrastructure` 子树

目标实现树：

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

## 3. 统一边界

### 3.1 不变的能力层

以下内容默认继续保持稳定：

- 注解入口仍然使用 `@DistributedLock`
- `scene`、`key`、`keyExpression`、`expireTime`、`waitTime` 的语义继续保留
- 锁键解析 SPI 继续使用 `DistributedLockKeyResolver`
- 获取失败 SPI 继续使用 `DistributedLockAcquireFailureHandler`
- 配置根前缀继续使用 `getboot.lock.*`

### 3.2 只在实现层扩展

新增实现时，优先新增这些子树，而不是把技术栈词汇带进 `api`：

- `infrastructure.database.jdbc.*`
- `infrastructure.zookeeper.curator.*`

如果后续需要实现专属扩展点，也优先放在：

- `spi.database`
- `spi.zookeeper`

前提是这些扩展点确实需要暴露给业务方或其他模块；否则仍应留在实现层内部协作，不直接承诺为公开 SPI。

## 4. Key 策略统一

不同实现继续共用同一套业务键构成思路：

1. 先由 `DistributedLockKeyResolver` 解析业务键
2. 再由模块内部统一拼装完整锁键
3. 最后由具体实现决定如何映射到底层介质

建议继续保持完整锁键抽象：

```text
<key-prefix>:<scene>#<resolved-key>
```

原因：

- 业务日志、异常和监控里可以保持统一可读性
- 后续不同实现切换时，业务侧不需要重写 `scene` / `keyExpression`
- Redis、JDBC、ZooKeeper 都可以从同一套业务键派生底层资源名

下一阶段建议补一个模块内部公共辅助类，负责：

- 校验 `scene`
- 校验解析后的业务键不为空
- 统一生成完整锁键

这样后续新增实现时，不需要在每个切面或拦截器里重复拼接规则。

## 5. 失败策略统一

当前默认失败处理逻辑是：

- 获取失败后，调用 `DistributedLockAcquireFailureHandler`
- 默认实现抛出 `DistributedLockException`

这个出口应继续保留。原因很简单：

- 业务方已经有稳定覆盖点
- 不同实现的失败原因可以不同，但对业务侧的失败收口不必分裂

建议的统一规则：

- 所有实现都只通过 `DistributedLockAcquireFailureHandler` 向外暴露“未拿到锁”
- 默认实现继续抛 `DistributedLockException`
- 底层异常如果属于基础设施错误，例如 Redis/JDBC/ZooKeeper 连接异常，不应该伪装成“拿锁失败”，而应直接保留原始异常语义

也就是说，后续需要区分两类情况：

- 正常竞争失败
  走 `DistributedLockAcquireFailureHandler`
- 基础设施异常
  直接抛出或包装为实现异常，不走竞争失败处理器

## 6. 不同实现的语义对齐

新增实现时，不要求每个技术栈百分之百拥有相同底层特性，但需要尽量保证注解层语义一致。

推荐对齐原则：

| 语义 | Redis / Redisson | Database / JDBC | ZooKeeper / Curator |
| --- | --- | --- | --- |
| `scene` | 作为锁键命名空间 | 作为锁记录命名空间 | 作为节点命名空间 |
| `key` / `keyExpression` | 解析后拼 Redis 键 | 解析后拼业务锁主键 | 解析后拼 ZK 节点路径 |
| `waitTime=-1` | 使用当前 Redisson 默认阻塞行为 | 通过轮询重试模拟等待 | 通过 Curator 锁等待机制阻塞 |
| `expireTime=-1` | 使用 Redisson watchdog | 使用实现默认租约 / 超时策略 | 使用会话存活语义，不强制映射固定租约 |
| 获取失败 | 统一走失败处理器 | 统一走失败处理器 | 统一走失败处理器 |

这里最关键的不是“底层完全一样”，而是“业务看到的入口和失败出口尽量一样”。

## 7. 配置规划

当前已经有：

```yaml
getboot:
  lock:
    enabled: true
    redis:
      enabled: true
      key-prefix: distributed_lock
```

后续建议继续沿用能力根前缀，并新增实现子树：

```yaml
getboot:
  lock:
    enabled: true
    redis:
      enabled: true
      key-prefix: distributed_lock
    database:
      enabled: false
      key-prefix: distributed_lock
      table-name: distributed_lock
      lease-ms: 30000
      retry-interval-ms: 100
    zookeeper:
      enabled: false
      key-prefix: distributed_lock
      base-path: /getboot/lock
```

规划约束：

- `enabled` 仍然保留在 `getboot.lock.*`
- 各实现使用各自子树，不复用第三方原生前缀
- 如果后续确实需要桥接到底层原生前缀，应放在实现层 `environment` 或配套模块，而不是把业务入口直接暴露成底层前缀

## 8. 代码落位建议

建议先补充共性支撑，再增加新实现。

推荐顺序：

1. 抽出完整锁键构建与基础校验的共性 support
2. 明确竞争失败与基础设施异常的统一边界
3. 增加 `infrastructure.database.jdbc.*`
4. 增加 `infrastructure.zookeeper.curator.*`
5. 更新模块 README 与能力矩阵

建议的实现层目录：

```text
com.getboot.lock.infrastructure.database.jdbc
├── autoconfigure
├── properties
├── repository
└── support

com.getboot.lock.infrastructure.zookeeper.curator
├── autoconfigure
├── properties
└── support
```

如果未来数据库锁需要额外表结构管理或轮询组件，这些都应继续留在 `infrastructure.database.jdbc.*` 内，不上浮到 `api`。

## 9. 下一步落地顺序

按当前 TODO 的优先级，建议这样推进：

1. 先抽统一锁键与失败处理基线
2. 先做 `database.jdbc`
   原因：数据库锁更容易在没有 ZooKeeper 环境的团队里直接验证，也更适合先把“竞争失败 vs 基础设施异常”的边界打清楚
3. 再做 `zookeeper.curator`
   原因：ZooKeeper 锁更适合作为第二种实现来验证模块分层是否稳定，而不是第一步就把能力层做复杂
4. 最后补 README、能力矩阵和示例配置

## 10. 完成标准

这一段 TODO 可以认为真正完成，至少满足：

- `getboot-lock` 下新增 `database.jdbc` 与 `zookeeper.curator` 两个实现子树
- `@DistributedLock` 不需要因为新增实现而修改业务使用方式
- `DistributedLockKeyResolver` 与 `DistributedLockAcquireFailureHandler` 仍然是统一扩展点
- README 能明确说明当前默认实现、已支持实现和各自前置条件
- 不同实现的失败出口、完整锁键规则和配置根前缀已经写清楚
