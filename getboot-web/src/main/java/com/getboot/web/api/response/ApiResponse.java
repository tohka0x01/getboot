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
package com.getboot.web.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.getboot.exception.api.code.CommonErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应结果封装。
 *
 * @param <T> 响应数据类型
 * @author qiheng
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@ToString
@JsonPropertyOrder({"status", "code", "message", "data", "meta"})
public class ApiResponse<T> implements Serializable {
    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final Integer SUCCESS_CODE = CommonErrorCode.SUCCESS.code();

    /**
     * 系统错误码
     */
    public static final Integer SYSTEM_ERROR_CODE = CommonErrorCode.ERROR.code();

    /**
     * 默认成功提示信息
     */
    public static final String DEFAULT_SUCCESS_MESSAGE = CommonErrorCode.SUCCESS.message();

    /**
     * 默认系统错误提示信息
     */
    public static final String DEFAULT_SYSTEM_ERROR_MESSAGE = CommonErrorCode.ERROR.message();

    /**
     * 响应元信息时间格式化器。
     */
    private static final DateTimeFormatter META_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 成功状态字符串
     */
    public static final String SUCCESS_STATUS = "success";

    /**
     * 失败状态字符串
     */
    public static final String FAIL_STATUS = "fail";

    /**
     * 响应状态：success/fail
     */
    private String status = SUCCESS_STATUS;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应状态码
     */
    private Integer code = SUCCESS_CODE;

    /**
     * 响应信息
     */
    private String message = DEFAULT_SUCCESS_MESSAGE;

    /**
     * 响应元信息。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetaInfo meta;

    /**
     * 响应元信息。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @Accessors(chain = true)
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetaInfo implements Serializable {
        /**
         * 序列化版本号。
         */
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 链路追踪标识。
         */
        private String traceId;

        /**
         * 响应时间。
         */
        private String timestamp;

        /**
         * 请求处理耗时（毫秒）。
         */
        private Long costMillis;

        /**
         * 创建默认元信息对象。
         *
         * @return 元信息对象
         */
        public static MetaInfo create() {
            return new MetaInfo().setTimestamp(LocalDateTime.now().format(META_TIME_FORMATTER));
        }
    }

    /**
     * 私有构造器
     */
    private ApiResponse(String status, T data, Integer code, String message) {
        this.status = validateStatus(status);
        this.data = data;
        this.code = code;
        this.message = message;
    }

    /**
     * 判断响应是否成功。
     *
     * @return 是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESS_STATUS.equals(this.status);
    }

    /**
     * 判断响应是否失败。
     *
     * @return 是否失败
     */
    @JsonIgnore
    public boolean isFail() {
        return FAIL_STATUS.equals(this.status);
    }

    /**
     * 设置链路追踪标识。
     *
     * @param traceId 链路追踪标识
     * @return 当前对象
     */
    public ApiResponse<T> setTraceId(String traceId) {
        this.ensureMeta().setTraceId(traceId);
        return this;
    }

    /**
     * 设置响应时间。
     *
     * @param timestamp 响应时间
     * @return 当前对象
     */
    public ApiResponse<T> setTimestamp(String timestamp) {
        this.ensureMeta().setTimestamp(timestamp);
        return this;
    }

    /**
     * 设置请求处理耗时。
     *
     * @param costMillis 请求处理耗时（毫秒）
     * @return 当前对象
     */
    public ApiResponse<T> setCostMillis(Long costMillis) {
        this.ensureMeta().setCostMillis(costMillis);
        return this;
    }

    /**
     * 设置追踪ID。
     *
     * @param tid 追踪ID
     * @return 当前对象
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    @JsonIgnore
    public ApiResponse<T> setTid(String tid) {
        return this.setTraceId(tid);
    }

    /**
     * 设置耗时。
     *
     * @param cost 耗时（毫秒）
     * @return 当前对象
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    @JsonIgnore
    public ApiResponse<T> setCost(Long cost) {
        return this.setCostMillis(cost);
    }

    /**
     * 获取旧版调试对象兼容视图。
     *
     * @return 响应元信息
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    @JsonIgnore
    public MetaInfo getDebug() {
        return this.meta;
    }

    /**
     * 设置旧版调试对象兼容视图。
     *
     * @param debug 旧版调试对象
     * @return 当前对象
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    @JsonIgnore
    public ApiResponse<T> setDebug(MetaInfo debug) {
        this.meta = debug;
        return this;
    }

    // ==================== 成功响应静态方法 ====================

    /**
     * 成功响应（无数据，使用默认成功信息）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(SUCCESS_STATUS, null, SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE);
    }

    /**
     * 成功响应（带数据，使用默认成功信息）
     *
     * @param data 响应数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_STATUS, data, SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE);
    }

    /**
     * 成功响应（带数据，自定义提示信息）
     *
     * @param data 响应数据
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(SUCCESS_STATUS, data, SUCCESS_CODE, message);
    }

    /**
     * 成功响应（自定义状态码和提示信息）
     *
     * @param code 状态码
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> success(Integer code, String message) {
        return new ApiResponse<>(SUCCESS_STATUS, null, code, message);
    }

    /**
     * 成功响应（带数据，自定义状态码）
     *
     * @param data 响应数据
     * @param code 状态码
     */
    public static <T> ApiResponse<T> success(T data, Integer code) {
        return new ApiResponse<>(SUCCESS_STATUS, data, code, DEFAULT_SUCCESS_MESSAGE);
    }

    /**
     * 成功响应（带数据，自定义状态码和提示信息）
     *
     * @param data 响应数据
     * @param code 状态码
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> success(T data, Integer code, String message) {
        return new ApiResponse<>(SUCCESS_STATUS, data, code, message);
    }

    // ==================== 失败响应静态方法 ====================

    /**
     * 失败响应（无数据，使用默认系统错误信息）
     */
    public static <T> ApiResponse<T> fail() {
        return new ApiResponse<>(FAIL_STATUS, null, SYSTEM_ERROR_CODE, DEFAULT_SYSTEM_ERROR_MESSAGE);
    }

    /**
     * 失败响应（带数据，使用默认系统错误信息）
     *
     * @param data 响应数据
     */
    public static <T> ApiResponse<T> fail(T data) {
        return new ApiResponse<>(FAIL_STATUS, data, SYSTEM_ERROR_CODE, DEFAULT_SYSTEM_ERROR_MESSAGE);
    }

    /**
     * 失败响应（自定义提示信息）
     *
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(FAIL_STATUS, null, SYSTEM_ERROR_CODE, message);
    }

    /**
     * 失败响应（带数据，自定义提示信息）
     *
     * @param data 响应数据
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> fail(T data, String message) {
        return new ApiResponse<>(FAIL_STATUS, data, SYSTEM_ERROR_CODE, message);
    }

    /**
     * 失败响应（自定义状态码和提示信息）
     *
     * @param code 状态码
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(FAIL_STATUS, null, code, message);
    }

    /**
     * 失败响应（带数据，自定义状态码）
     *
     * @param data 响应数据
     * @param code 状态码
     */
    public static <T> ApiResponse<T> fail(T data, Integer code) {
        return new ApiResponse<>(FAIL_STATUS, data, code, DEFAULT_SYSTEM_ERROR_MESSAGE);
    }

    /**
     * 失败响应（带数据，自定义状态码和提示信息）
     *
     * @param data 响应数据
     * @param code 状态码
     * @param message 提示信息
     */
    public static <T> ApiResponse<T> fail(T data, Integer code, String message) {
        return new ApiResponse<>(FAIL_STATUS, data, code, message);
    }

    // ==================== 链式调用方法 ====================

    /**
     * 设置响应数据
     *
     * @param data 响应数据
     * @return 当前对象
     */
    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * 设置提示信息
     *
     * @param message 提示信息
     * @return 当前对象
     */
    public ApiResponse<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * 设置状态码
     *
     * @param code 状态码
     * @return 当前对象
     */
    public ApiResponse<T> setCode(Integer code) {
        this.code = code;
        return this;
    }

    /**
     * 设置响应状态
     *
     * @param status 响应状态
     * @return 当前对象
     */
    public ApiResponse<T> setStatus(String status) {
        this.status = validateStatus(status);
        return this;
    }

    /**
     * 设置响应元信息。
     *
     * @param meta 响应元信息
     * @return 当前对象
     */
    public ApiResponse<T> setMeta(MetaInfo meta) {
        this.meta = meta;
        return this;
    }

    /**
     * 获取响应元信息，不存在时自动初始化。
     *
     * @return 响应元信息
     */
    private MetaInfo ensureMeta() {
        if (this.meta == null) {
            this.meta = MetaInfo.create();
        }
        return this.meta;
    }

    /**
     * 校验响应状态值。
     *
     * @param status 响应状态
     * @return 标准状态值
     */
    private static String validateStatus(String status) {
        if (SUCCESS_STATUS.equals(status) || FAIL_STATUS.equals(status)) {
            return status;
        }
        throw new IllegalArgumentException("Response status must be either success or fail.");
    }
}
