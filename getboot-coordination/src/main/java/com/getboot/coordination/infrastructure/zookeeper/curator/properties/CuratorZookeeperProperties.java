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
package com.getboot.coordination.infrastructure.zookeeper.curator.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Curator ZooKeeper 配置。
 *
 * @author qiheng
 */
@Data
@ConfigurationProperties(prefix = "getboot.coordination.zookeeper")
public class CuratorZookeeperProperties {

    /**
     * 是否启用 ZooKeeper / Curator。
     */
    private boolean enabled = false;

    /**
     * ZooKeeper 连接地址。
     */
    private String connectString;

    /**
     * ZooKeeper 命名空间。
     */
    private String namespace;

    /**
     * 会话超时时间，单位毫秒。
     */
    private int sessionTimeoutMs = 60000;

    /**
     * 连接超时时间，单位毫秒。
     */
    private int connectionTimeoutMs = 15000;

    /**
     * 重试配置。
     */
    private Retry retry = new Retry();

    /**
     * Curator 重试配置。
     */
    @Data
    public static class Retry {

        /**
         * 首次重试休眠时间，单位毫秒。
         */
        private int baseSleepTimeMs = 1000;

        /**
         * 最大重试次数。
         */
        private int maxRetries = 3;

        /**
         * 最大休眠时间，单位毫秒。
         */
        private int maxSleepMs = 8000;
    }
}
