package com.github.teranes10.androidutils.helpers;

import com.github.teranes10.androidutils.models.Result;

import java.util.concurrent.CompletableFuture;

public abstract class UseCase<T> {

    protected abstract Result<T> onExecute();

    public CompletableFuture<Result<T>> execute() {
        return CompletableFuture.supplyAsync(this::onExecute);
    }
}
