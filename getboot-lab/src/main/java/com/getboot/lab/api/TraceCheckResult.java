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
package com.getboot.lab.api;

/**
 * Trace 验证结果
 *
 * @param requestHeader 请求头 TraceId
 * @param requestAttribute 请求属性 TraceId
 * @param contextTraceId 当前上下文 TraceId
 * @param mdcTraceId 日志 MDC TraceId
 *
 * @author qiheng
 */
public record TraceCheckResult(
        String requestHeader,
        Object requestAttribute,
        String contextTraceId,
        String mdcTraceId) {
}
