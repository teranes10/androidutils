package com.github.teranes10.androidutils.utils.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Dao
public abstract class BaseDao<T> {

    @RawQuery
    protected abstract List<T> doGetAll(SupportSQLiteQuery query);

    public List<T> getAll() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(
                "SELECT * FROM " + getTableName());
        return doGetAll(query);
    }

    public CompletableFuture<List<T>> getAllAsync() {
        return CompletableFuture.supplyAsync(this::getAll);
    }

    @RawQuery
    protected abstract List<T> doPagination(SupportSQLiteQuery query);

    public Pagination<T> pagination(int page, int itemsPerPage) {
        int offset = (page - 1) * itemsPerPage;

        SimpleSQLiteQuery query = new SimpleSQLiteQuery(
                "SELECT * FROM " + getTableName() +
                        " LIMIT " + offset + ", " + itemsPerPage);

        return new Pagination<>(
                doPagination(query),
                (int) count()
        );
    }

    public CompletableFuture<Pagination<T>> paginationAsync(int page, int itemsPerPage) {
        return CompletableFuture.supplyAsync(() -> pagination(page, itemsPerPage));
    }

    @RawQuery
    protected abstract long doCount(SupportSQLiteQuery query);

    public long count() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(
                "SELECT COUNT(*) FROM " + getTableName());
        return doCount(query);
    }

    public CompletableFuture<Long> countAsync() {
        return CompletableFuture.supplyAsync(this::count);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract long doSave(T entity);

    public long save(T entity) {
        return doSave(entity);
    }

    public CompletableFuture<Long> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> save(entity));
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract long[] doSave(List<T> entities);

    public long[] save(List<T> entities) {
        return doSave(entities);
    }

    public CompletableFuture<long[]> saveAsync(List<T> entities) {
        return CompletableFuture.supplyAsync(() -> save(entities));
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract long doInsert(T entity);

    public long insert(T entity) {
        return doInsert(entity);
    }

    public CompletableFuture<Long> insertAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> insert(entity));
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract long[] doInsert(List<T> entities);

    public long[] insert(List<T> entities) {
        return doInsert(entities);
    }

    public CompletableFuture<long[]> insertAsync(List<T> entities) {
        return CompletableFuture.supplyAsync(() -> insert(entities));
    }

    @Delete
    protected abstract int doDelete(T entity);

    public boolean delete(T entity) {
        return doDelete(entity) > 0;
    }

    public CompletableFuture<Boolean> deleteAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> delete(entity));
    }

    @Delete
    protected abstract int doDelete(List<T> entities);

    public boolean delete(List<T> entities) {
        return doDelete(entities) > 0;
    }

    public CompletableFuture<Boolean> deleteAsync(List<T> entities) {
        return CompletableFuture.supplyAsync(() -> delete(entities));
    }

    @RawQuery
    protected abstract long doDeleteAll(SupportSQLiteQuery query);

    public boolean deleteAll() {
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(
                "DELETE FROM " + getTableName()
        );
        return doDeleteAll(query) > 0;
    }

    public CompletableFuture<Boolean> deleteAllAsync() {
        return CompletableFuture.supplyAsync(this::deleteAll);
    }

    @RawQuery
    protected abstract long doDeleteByIds(SupportSQLiteQuery query);

    public <Tv extends Number> boolean deleteByIds(List<Tv> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            placeholders.append("?");
            if (i < ids.size() - 1) {
                placeholders.append(", ");
            }
        }

        String queryString = "DELETE FROM " + getTableName() + " WHERE id IN (" + placeholders + ")";
        SimpleSQLiteQuery query = new SimpleSQLiteQuery(queryString, ids.toArray());

        return doDeleteByIds(query) > 0;
    }

    public <Tv extends Number> CompletableFuture<Boolean> deleteByIdsAsync(List<Tv> ids) {
        return CompletableFuture.supplyAsync(() -> deleteByIds(ids));
    }

    protected String getTableName() {
        ParameterizedType parameterizedType = getClass().getSuperclass() != null ?
                ((ParameterizedType) getClass().getSuperclass().getGenericSuperclass()) : null;

        Class<?> tableClass = parameterizedType != null && parameterizedType.getActualTypeArguments().length > 0 ?
                (Class<?>) parameterizedType.getActualTypeArguments()[0] : null;

        return tableClass != null ? tableClass.getSimpleName() : "";
    }
}
