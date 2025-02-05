package com.github.teranes10.androidutils.models;

public class Result<T> {
    private final boolean success;
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

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ResultType getType() {
        return type;
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
                "success=" + success +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", type=" + type +
                '}';
    }
}
