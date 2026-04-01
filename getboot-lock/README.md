# getboot-lock

分布式锁 starter，当前提供基于 Redis / Redisson 的声明式加锁能力。

## 作用

- 提供声明式分布式锁注解 `@DistributedLock`
- 提供默认锁键解析与失败处理策略
- 提供基于 Redis / Redisson 的加锁切面实现

## 接入方式

业务项目继承父 `pom` 后，按需引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-lock</artifactId>
</dependency>
```

适合这几类场景：

- 想在业务方法上直接声明式加锁，而不是手工写 Redisson 模板代码
- 想统一锁 Key 生成规则、失败处理策略和配置前缀
- 后续可能继续扩展数据库锁、ZooKeeper 锁，但希望对外仍保持同一能力入口

## 前置条件

- 模块自身已经依赖 `getboot-coordination`，但业务环境仍需要准备可用的 Redis / Redisson
- 默认切面只有在 Spring 容器中存在 `RedissonClient` 时才会生效
- 至少需要准备 `getboot.coordination.redisson.*` 对应配置
- 若关闭 `getboot.lock.enabled` 或 `getboot.lock.redis.enabled`，默认锁切面不会注册

## 目录约定

- `api.*`：注解、异常、常量与配置模型
- `spi`：锁键解析与失败处理扩展点
- `support`：默认策略实现
- `infrastructure.redis.redisson.*`：Redis / Redisson 实现与自动装配

## 配置示例

```yaml
getboot:
  lock:
    enabled: true
    redis:
      enabled: true
      key-prefix: distributed_lock
  coordination:
    redisson:
      file: classpath:redisson/redisson.yaml # Redisson 配置文件位置
```

## 默认 Bean

- `DistributedLockKeyResolver`：默认实现为 `SpelDistributedLockKeyResolver`
- `DistributedLockAcquireFailureHandler`：默认实现为 `DefaultDistributedLockAcquireFailureHandler`
- `DistributedLockAspect`：默认 Redis / Redisson 分布式锁切面

## 扩展点

- 业务方法统一通过 `@DistributedLock` 进入，不需要直接依赖 Redisson API
- 可通过注册 `DistributedLockKeyResolver` Bean 自定义锁键生成规则
- 可通过注册 `DistributedLockAcquireFailureHandler` Bean 自定义锁获取失败处理逻辑
- 模块总开关保持在 `getboot.lock.enabled`
- Redis 锁实现级配置统一收敛到 `getboot.lock.redis.*`
- `getboot.lock.redis.key-prefix` 用于统一拼装 Redis 锁键前缀
- Redisson 基础设施配置继续复用 `getboot.coordination.redisson.*`
- 注解、异常与配置模型统一收敛在 `com.getboot.lock.api.*`
- Redis / Redisson 实现统一收敛在 `com.getboot.lock.infrastructure.redis.redisson.*`

## 已实现技术栈

- Redis Lock
- Redisson

## 边界 / 补充文档

- 当前只提供 Redis / Redisson 实现，数据库锁与 ZooKeeper 锁仍在路线图中
- `getboot-lock` 负责能力层、注解和切面，不负责 `RedissonClient` 的底层装配；相关基础设施仍由 `getboot-coordination` 承接
- `getboot.lock.*` 是模块自己的 `@ConfigurationProperties`，`getboot.coordination.redisson.*` 请继续参考 `getboot-coordination/README.md`
- 可直接参考 `src/main/resources/getboot-lock.yml.example`
- 如果你关心下一阶段的数据库锁 / ZooKeeper 锁扩展，而不是当前接入方式，先看主 README，再看 [`docs/LOCK_EVOLUTION_PLAN.md`](./docs/LOCK_EVOLUTION_PLAN.md)
