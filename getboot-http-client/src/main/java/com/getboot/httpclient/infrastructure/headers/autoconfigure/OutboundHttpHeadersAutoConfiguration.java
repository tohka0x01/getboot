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
package com.getboot.httpclient.infrastructure.headers.autoconfigure;

import com.getboot.httpclient.spi.OutboundHttpHeadersContributor;
import com.getboot.httpclient.support.headers.OutboundHttpHeadersResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * 通用出站 HTTP 请求头自动配置。
 *
 * <p>负责注册模块级公共请求头解析器，供不同客户端实现复用。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(HttpHeaders.class)
public class OutboundHttpHeadersAutoConfiguration {

    /**
     * 注册出站请求头解析器。
     *
     * @param contributors 通用请求头贡献器
     * @return 请求头解析器
     */
    @Bean(name = "getbootOutboundHttpHeadersResolver")
    @ConditionalOnMissingBean(name = "getbootOutboundHttpHeadersResolver")
    public OutboundHttpHeadersResolver getbootOutboundHttpHeadersResolver(
            ObjectProvider<OutboundHttpHeadersContributor> contributors) {
        return new OutboundHttpHeadersResolver(contributors.orderedStream().toList());
    }
}
