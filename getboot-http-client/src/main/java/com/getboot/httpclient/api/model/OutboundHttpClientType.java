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
package com.getboot.httpclient.api.model;

/**
 * 出站 HTTP 客户端类型。
 *
 * <p>用于在模块通用出站头扩展点中区分当前请求来自哪一类客户端实现。</p>
 *
 * @author qiheng
 */
public enum OutboundHttpClientType {

    /**
     * OpenFeign 客户端。
     */
    OPEN_FEIGN,

    /**
     * WebClient 客户端。
     */
    WEB_CLIENT,

    /**
     * RestTemplate 客户端。
     */
    REST_TEMPLATE
}
