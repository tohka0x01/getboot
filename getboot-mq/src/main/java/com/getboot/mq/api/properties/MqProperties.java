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
package com.getboot.mq.api.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ 模块配置。
 *
 * <p>用于统一控制当前启用的 MQ 实现类型。</p>
 *
 * @author qiheng
 */
@Data
@ConfigurationProperties(prefix = "getboot.mq")
public class MqProperties {

    /**
     * 是否启用 MQ 能力模块。
     */
    private boolean enabled = true;

    /**
     * 当前启用的 MQ 类型。
     */
    private String type = "rocketmq";

    /**
     * RocketMQ 能力开关配置。
     */
    private Rocketmq rocketmq = new Rocketmq();

    /**
     * Kafka 能力开关配置。
     */
    private Kafka kafka = new Kafka();

    /**
     * MQTT 能力开关配置。
     */
    private Mqtt mqtt = new Mqtt();

    /**
     * RocketMQ 相关能力开关配置。
     *
     * @author qiheng
     */
    @Data
    public static class Rocketmq {

        /**
         * 是否启用 RocketMQ 实现。
         */
        private boolean enabled = true;
    }

    /**
     * Kafka 相关能力开关配置。
     *
     * @author qiheng
     */
    @Data
    public static class Kafka {

        /**
         * 是否启用 Kafka 实现。
         */
        private boolean enabled = false;
    }

    /**
     * MQTT 相关能力开关配置。
     *
     * @author qiheng
     */
    @Data
    public static class Mqtt {

        /**
         * 是否启用 MQTT 实现。
         */
        private boolean enabled = false;

        /**
         * MQTT Broker 地址。
         */
        private String serverUri = "tcp://127.0.0.1:1883";

        /**
         * MQTT 客户端标识。
         */
        private String clientId = "getboot-mqtt";

        /**
         * MQTT 用户名。
         */
        private String username;

        /**
         * MQTT 密码。
         */
        private String password;

        /**
         * 默认 QoS 级别。
         */
        private int defaultQos = 1;

        /**
         * 默认是否发送 retained 消息。
         */
        private boolean retained;

        /**
         * 是否启用异步发送确认。
         */
        private boolean async;

        /**
         * 连接超时时间，单位毫秒。
         */
        private int connectTimeoutMs = 5000;

        /**
         * MQTT KeepAlive 秒数。
         */
        private int keepAliveSeconds = 60;
    }
}
