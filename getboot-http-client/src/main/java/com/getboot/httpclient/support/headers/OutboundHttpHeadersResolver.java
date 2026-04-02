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
package com.getboot.httpclient.support.headers;

import com.getboot.httpclient.api.model.OutboundHttpRequestContext;
import com.getboot.httpclient.spi.OutboundHttpHeadersContributor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 出站 HTTP 请求头解析器。
 *
 * <p>统一处理模块通用请求头贡献器与 Trace 请求头注入逻辑，供不同 HTTP 客户端实现复用。</p>
 *
 * @author qiheng
 */
public class OutboundHttpHeadersResolver {

    private final List<OutboundHttpHeadersContributor> contributors;

    public OutboundHttpHeadersResolver(List<OutboundHttpHeadersContributor> contributors) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }

    /**
     * 解析当前请求需要注入的请求头。
     *
     * @param context 出站请求上下文
     * @return 解析后的请求头
     */
    public HttpHeaders resolve(OutboundHttpRequestContext context) {
        HttpHeaders headers = new HttpHeaders();
        contribute(headers, context);
        return headers;
    }

    /**
     * 将公共请求头和 Trace 请求头写入到目标请求头中。
     *
     * @param headers 目标请求头
     * @param context 出站请求上下文
     */
    public void contribute(HttpHeaders headers, OutboundHttpRequestContext context) {
        Assert.notNull(headers, "headers must not be null");
        Assert.notNull(context, "context must not be null");

        contributors.forEach(contributor -> contributor.contribute(headers, context));

        if (StringUtils.hasText(context.getTraceId()) && StringUtils.hasText(context.getTraceHeaderName())) {
            headers.remove(context.getTraceHeaderName());
            headers.add(context.getTraceHeaderName(), context.getTraceId());
        }
    }
}
