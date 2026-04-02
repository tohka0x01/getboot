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
package com.getboot.httpclient.spi;

import com.getboot.httpclient.api.model.OutboundHttpRequestContext;
import org.springframework.http.HttpHeaders;

/**
 * 通用出站 HTTP 请求头贡献器。
 *
 * <p>适用于租户标识、应用标识、语言环境等不依赖具体客户端类型的公共请求头增强。</p>
 *
 * @author qiheng
 */
@FunctionalInterface
public interface OutboundHttpHeadersContributor {

    /**
     * 向当前出站请求补充请求头。
     *
     * @param headers 待写入的请求头
     * @param context 当前出站请求上下文
     */
    void contribute(HttpHeaders headers, OutboundHttpRequestContext context);
}
