package com.github.teranes10.androidutils.models;

import static com.github.teranes10.androidutils.utils.Utils.getOrDefault;

public class Result<T> {
    private boolean isSuccess;
    private final boolean success;
    private String status;
    private final String message;
    private final T data;
    private ResultType type = ResultType.Unknown;

    public Result(boolean success, T data, String message, ResultType type) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.type = type;
    }

    public Result(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public Boolean isSuccess() {
        return (status != null && status.equals("success")) || success || isSuccess;
    }

    public ResultType getType() {
        return type;
    }

    public String getMessage() {
        return getOrDefault(this.message);
    }

    public T getData() {
        return data;
    }


    private static <T> Result<T> create(boolean success, T data, String message, ResultType type) {
        return new Result<>(success, data, message, type);
    }

    private static <T> Result<T> create(boolean success, T data, String message) {
        return Result.create(success, data, message, ResultType.Unknown);
    }

    public static <T> Result<T> ok(T data) {
        return Result.create(true, data, "");
    }

    public static <T> Result<T> ok(T data, String message) {
        return Result.create(true, data, message);
    }

    public static <T> Result<T> fail(String message) {
        return Result.create(false, null, message);
    }

    public static <T> Result<T> fail(T data, String message) {
        return Result.create(false, null, message);
    }

    public static <T> Result<T> fail(String message, ResultType type) {
        return Result.create(false, null, message, type);
    }

    public enum ResultType {
        InvalidInput,
        Unauthorized,
        NotFound,
        Unknown,
        NoInternetConnection
    }

    @Override
    public String toString() {
        return "Result{" +
                "isSuccess=" + isSuccess +
                ", success=" + success +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", type=" + type +
                '}';
    }
}
