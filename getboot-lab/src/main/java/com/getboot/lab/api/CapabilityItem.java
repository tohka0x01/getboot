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
 * 能力验证项
 *
 * @param module 模块名
 * @param name 能力名称
 * @param category 能力分组
 * @param status 当前验证状态
 * @param description 能力说明
 * @param actionPath 可手动验证的接口
 * @param dependency 依赖环境
 *
 * @author qiheng
 */
public record CapabilityItem(
        String module,
        String name,
        String category,
        String status,
        String description,
        String actionPath,
        String dependency) {
}
