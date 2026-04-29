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

import java.util.List;

/**
 * 能力验证总览
 *
 * @param application 应用名
 * @param profiles 当前环境
 * @param javaVersion Java 版本
 * @param capabilities 能力清单
 *
 * @author qiheng
 */
public record CapabilityOverview(
        String application,
        List<String> profiles,
        String javaVersion,
        List<CapabilityItem> capabilities) {
}
