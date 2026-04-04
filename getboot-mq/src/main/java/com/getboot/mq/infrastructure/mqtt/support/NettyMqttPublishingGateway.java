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
package com.getboot.mq.infrastructure.mqtt.support;

import com.getboot.mq.api.properties.MqProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Netty 的 MQTT 发布网关。
 *
 * <p>当前只负责连接 Broker 并发送发布消息，不承担订阅编排。</p>
 *
 * @author qiheng
 */
public class NettyMqttPublishingGateway {

    /**
     * MQTT 能力配置。
     */
    private final MqProperties.Mqtt mqttProperties;

    /**
     * 创建 Netty MQTT 发布网关。
     *
     * @param mqttProperties MQTT 能力配置
     */
    public NettyMqttPublishingGateway(MqProperties.Mqtt mqttProperties) {
        this.mqttProperties = mqttProperties == null ? new MqProperties.Mqtt() : mqttProperties;
    }

    /**
     * 发布 MQTT 消息。
     *
     * @param topic 主题
     * @param payload 负载
     * @param qos QoS 级别
     * @param retained retained 标识
     */
    public void publish(String topic, byte[] payload, int qos, boolean retained) {
        Assert.hasText(topic, "MQTT topic must not be blank.");
        Assert.notNull(payload, "MQTT payload must not be null.");
        Assert.isTrue(qos == 0 || qos == 1, "MQTT QoS must be 0 or 1 in current implementation.");

        ServerEndpoint endpoint = ServerEndpoint.parse(mqttProperties.getServerUri());
        PublishState publishState = new PublishState();
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, mqttProperties.getConnectTimeoutMs())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(MqttEncoder.INSTANCE);
                            channel.pipeline().addLast(new MqttDecoder());
                            channel.pipeline().addLast(new MqttResponseHandler(publishState));
                        }
                    });

            Channel channel = bootstrap.connect(endpoint.host(), endpoint.port()).sync().channel();
            channel.writeAndFlush(buildConnectMessage()).sync();
            publishState.awaitConnected(mqttProperties.getConnectTimeoutMs());

            var publishBuilder = MqttMessageBuilders.publish()
                    .topicName(topic.trim())
                    .qos(MqttQoS.valueOf(qos))
                    .retained(retained)
                    .payload(Unpooled.wrappedBuffer(payload));
            if (qos > 0) {
                publishBuilder.messageId(1);
            }
            channel.writeAndFlush(publishBuilder.build()).sync();
            publishState.awaitPublished(qos, mqttProperties.getConnectTimeoutMs());

            channel.writeAndFlush(MqttMessageBuilders.disconnect().build()).sync();
            channel.close().sync();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MQTT publish was interrupted.", ex);
        } finally {
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    /**
     * 构建 MQTT CONNECT 报文。
     *
     * @return CONNECT 报文
     */
    private MqttMessage buildConnectMessage() {
        var connectBuilder = MqttMessageBuilders.connect()
                .protocolVersion(MqttVersion.MQTT_3_1_1)
                .clientId(resolveClientId())
                .cleanSession(true)
                .keepAlive(mqttProperties.getKeepAliveSeconds())
                .hasUser(StringUtils.hasText(mqttProperties.getUsername()))
                .hasPassword(mqttProperties.getPassword() != null);
        if (StringUtils.hasText(mqttProperties.getUsername())) {
            connectBuilder.username(mqttProperties.getUsername().trim());
        }
        if (mqttProperties.getPassword() != null) {
            connectBuilder.password(mqttProperties.getPassword());
        }
        return connectBuilder.build();
    }

    /**
     * 解析客户端标识。
     *
     * @return 客户端标识
     */
    private String resolveClientId() {
        if (StringUtils.hasText(mqttProperties.getClientId())) {
            return mqttProperties.getClientId().trim();
        }
        return "getboot-mqtt";
    }

    /**
     * MQTT 服务端点。
     *
     * @param host 主机
     * @param port 端口
     */
    private record ServerEndpoint(String host, int port) {

        /**
         * 从 URI 解析服务端点。
         *
         * @param serverUri MQTT 服务 URI
         * @return 服务端点
         */
        private static ServerEndpoint parse(String serverUri) {
            Assert.hasText(serverUri, "MQTT server URI must not be blank.");
            URI uri = URI.create(serverUri.trim());
            String scheme = uri.getScheme();
            Assert.isTrue("tcp".equalsIgnoreCase(scheme), "MQTT server URI must use tcp scheme.");
            Assert.hasText(uri.getHost(), "MQTT server host must not be blank.");
            int port = uri.getPort() > 0 ? uri.getPort() : 1883;
            return new ServerEndpoint(uri.getHost(), port);
        }
    }

    /**
     * MQTT 发布状态控制器。
     */
    private static final class PublishState {

        /**
         * 连接确认等待器。
         */
        private final CountDownLatch connectLatch = new CountDownLatch(1);

        /**
         * 发布确认等待器。
         */
        private final CountDownLatch publishLatch = new CountDownLatch(1);

        /**
         * 最近一次连接返回码。
         */
        private final AtomicReference<MqttConnectReturnCode> connectReturnCode = new AtomicReference<>();

        /**
         * 最近一次失败原因。
         */
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        /**
         * 记录连接确认。
         *
         * @param returnCode 连接返回码
         */
        private void onConnAck(MqttConnectReturnCode returnCode) {
            connectReturnCode.set(returnCode);
            connectLatch.countDown();
        }

        /**
         * 记录发布确认。
         */
        private void onPubAck() {
            publishLatch.countDown();
        }

        /**
         * 记录失败。
         *
         * @param throwable 失败原因
         */
        private void onFailure(Throwable throwable) {
            failure.compareAndSet(null, throwable);
            connectLatch.countDown();
            publishLatch.countDown();
        }

        /**
         * 等待连接确认。
         *
         * @param timeoutMs 超时时间
         * @throws InterruptedException 等待中断
         */
        private void awaitConnected(int timeoutMs) throws InterruptedException {
            if (!connectLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timed out waiting for MQTT CONNACK.");
            }
            if (failure.get() != null) {
                throw new IllegalStateException("Failed to connect MQTT broker.", failure.get());
            }
            if (connectReturnCode.get() != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                throw new IllegalStateException("MQTT broker rejected connection. returnCode=" + connectReturnCode.get());
            }
        }

        /**
         * 等待发布确认。
         *
         * @param qos QoS 级别
         * @param timeoutMs 超时时间
         * @throws InterruptedException 等待中断
         */
        private void awaitPublished(int qos, int timeoutMs) throws InterruptedException {
            if (failure.get() != null) {
                throw new IllegalStateException("Failed to publish MQTT message.", failure.get());
            }
            if (qos == 0) {
                return;
            }
            if (!publishLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Timed out waiting for MQTT PUBACK.");
            }
            if (failure.get() != null) {
                throw new IllegalStateException("Failed to publish MQTT message.", failure.get());
            }
        }
    }

    /**
     * MQTT 响应处理器。
     */
    private static final class MqttResponseHandler extends SimpleChannelInboundHandler<MqttMessage> {

        /**
         * 发布状态控制器。
         */
        private final PublishState publishState;

        /**
         * 创建响应处理器。
         *
         * @param publishState 发布状态控制器
         */
        private MqttResponseHandler(PublishState publishState) {
            this.publishState = publishState;
        }

        /**
         * 处理入站 MQTT 报文。
         *
         * @param context 通道上下文
         * @param message MQTT 报文
         */
        @Override
        protected void channelRead0(ChannelHandlerContext context, MqttMessage message) {
            if (message.fixedHeader().messageType() == MqttMessageType.CONNACK) {
                publishState.onConnAck(((MqttConnAckMessage) message).variableHeader().connectReturnCode());
                return;
            }
            if (message.fixedHeader().messageType() == MqttMessageType.PUBACK) {
                publishState.onPubAck();
            }
        }

        /**
         * 捕获异常并中断发布流程。
         *
         * @param context 通道上下文
         * @param cause 异常原因
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            publishState.onFailure(cause);
            context.close();
        }
    }
}
