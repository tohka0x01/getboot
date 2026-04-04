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
package com.getboot.websocket.support.sender;

import com.getboot.websocket.api.sender.WebSocketMessageSender;
import com.getboot.websocket.support.WebSocketSessionAttributes;
import com.getboot.websocket.support.registry.DefaultWebSocketSessionRegistry;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 默认 WebSocket 消息推送器测试。
 *
 * @author qiheng
 */
class DefaultWebSocketMessageSenderTest {

    /**
     * 验证默认推送器支持按会话、按用户和广播发送文本消息。
     */
    @Test
    void shouldSendMessagesBySessionUserAndBroadcast() {
        DefaultWebSocketSessionRegistry sessionRegistry = new DefaultWebSocketSessionRegistry();
        TestSession alice = new TestSession("s1", true);
        alice.getUserProperties().put(WebSocketSessionAttributes.USER_ID, "alice");
        TestSession bob = new TestSession("s2", true);
        bob.getUserProperties().put(WebSocketSessionAttributes.USER_ID, "bob");
        sessionRegistry.register(alice);
        sessionRegistry.register(bob);

        WebSocketMessageSender sender = new DefaultWebSocketMessageSender(sessionRegistry);

        assertEquals(1, sender.sendToSession("s1", "hello-session"));
        assertEquals("hello-session", alice.asyncRemote.lastTextMessage);

        assertEquals(1, sender.sendToUser("bob", Map.of("event", "notice")));
        assertEquals("{\"event\":\"notice\"}", bob.asyncRemote.lastTextMessage);

        assertEquals(2, sender.broadcast("hello-all"));
        assertEquals("hello-all", alice.asyncRemote.lastTextMessage);
        assertEquals("hello-all", bob.asyncRemote.lastTextMessage);
    }

    /**
     * 测试用 Session。
     */
    private static final class TestSession implements Session {

        /**
         * 会话 ID。
         */
        private final String id;

        /**
         * 当前会话是否打开。
         */
        private final boolean open;

        /**
         * 用户属性。
         */
        private final Map<String, Object> userProperties = new java.util.concurrent.ConcurrentHashMap<>();

        /**
         * 测试用异步发送端。
         */
        private final TestAsyncRemote asyncRemote = new TestAsyncRemote();

        /**
         * 创建测试用会话。
         *
         * @param id 会话 ID
         * @param open 是否打开
         */
        private TestSession(String id, boolean open) {
            this.id = id;
            this.open = open;
        }

        @Override
        public RemoteEndpoint.Async getAsyncRemote() {
            return asyncRemote;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public Map<String, Object> getUserProperties() {
            return userProperties;
        }

        @Override
        public Map<String, List<String>> getRequestParameterMap() {
            return Map.of();
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public URI getRequestURI() {
            return URI.create("ws://localhost/test");
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public Set<Session> getOpenSessions() {
            return Set.of(this);
        }

        @Override
        public jakarta.websocket.WebSocketContainer getContainer() {
            return null;
        }

        @Override
        public void addMessageHandler(MessageHandler handler) {
        }

        @Override
        public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
        }

        @Override
        public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
        }

        @Override
        public Set<MessageHandler> getMessageHandlers() {
            return Set.of();
        }

        @Override
        public void removeMessageHandler(MessageHandler handler) {
        }

        @Override
        public String getProtocolVersion() {
            return "13";
        }

        @Override
        public String getNegotiatedSubprotocol() {
            return null;
        }

        @Override
        public List<Extension> getNegotiatedExtensions() {
            return List.of();
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public long getMaxIdleTimeout() {
            return 0;
        }

        @Override
        public void setMaxIdleTimeout(long timeout) {
        }

        @Override
        public void setMaxBinaryMessageBufferSize(int length) {
        }

        @Override
        public int getMaxBinaryMessageBufferSize() {
            return 0;
        }

        @Override
        public void setMaxTextMessageBufferSize(int length) {
        }

        @Override
        public int getMaxTextMessageBufferSize() {
            return 0;
        }

        @Override
        public RemoteEndpoint.Basic getBasicRemote() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void close(CloseReason closeReason) throws IOException {
        }

        @Override
        public Map<String, String> getPathParameters() {
            return Map.of();
        }
    }

    /**
     * 测试用异步发送端。
     */
    private static final class TestAsyncRemote implements RemoteEndpoint.Async {

        /**
         * 最近一次发送的文本消息。
         */
        private String lastTextMessage;

        /**
         * 当前发送超时时间。
         */
        private long sendTimeout;

        @Override
        public long getSendTimeout() {
            return sendTimeout;
        }

        @Override
        public void setSendTimeout(long timeout) {
            this.sendTimeout = timeout;
        }

        @Override
        public void sendText(String text, SendHandler completion) {
            this.lastTextMessage = text;
        }

        @Override
        public Future<Void> sendText(String text) {
            this.lastTextMessage = text;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Future<Void> sendBinary(ByteBuffer data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void sendBinary(ByteBuffer data, SendHandler completion) {
        }

        @Override
        public Future<Void> sendObject(Object data) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void sendObject(Object data, SendHandler completion) {
        }

        @Override
        public void setBatchingAllowed(boolean batchingAllowed) {
        }

        @Override
        public boolean getBatchingAllowed() {
            return false;
        }

        @Override
        public void flushBatch() {
        }

        @Override
        public void sendPing(ByteBuffer applicationData) {
        }

        @Override
        public void sendPong(ByteBuffer applicationData) {
        }
    }
}
