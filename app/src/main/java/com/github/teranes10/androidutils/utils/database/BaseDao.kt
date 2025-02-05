package com.github.teranes10.androidutils.utils.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class BaseDao<T>(private val tableName: String) {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun save(item: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun save(items: List<T>): LongArray

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(item: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(items: List<T>): LongArray

    @Delete
    abstract suspend fun doDelete(item: T): Int

    @Delete
    abstract suspend fun doDelete(items: List<T>): Int

    @RawQuery
    abstract suspend fun doGetAll(query: SupportSQLiteQuery): List<T>

    @RawQuery
    abstract suspend fun doGet(query: SupportSQLiteQuery): T?

    @RawQuery
    abstract suspend fun doScalar(query: SupportSQLiteQuery): Long

    private fun buildQuery(query: String, vararg args: Any): SupportSQLiteQuery {
        return SimpleSQLiteQuery(query, args)
    }

    suspend fun getAll(): List<T> {
        val query = buildQuery("SELECT * FROM $tableName")
        return doGetAll(query)
    }

    suspend fun getById(id: Long): T? {
        val query = buildQuery("SELECT * FROM $tableName WHERE id = ? LIMIT 1", id)
        return doGet(query)
    }

    suspend fun getFirst(): T? {
        val query = buildQuery("SELECT * FROM $tableName ORDER BY id ASC LIMIT 1")
        return doGet(query)
    }

    suspend fun getLast(): T? {
        val query = buildQuery("SELECT * FROM $tableName ORDER BY id DESC LIMIT 1")
        return doGet(query)
    }

    suspend fun count(): Long {
        val query = buildQuery("SELECT COUNT(*) FROM $tableName")
        return doScalar(query)
    }

    suspend fun pagination(page: Int, pageSize: Int): Pagination<T> {
        val offset = (page - 1) * pageSize
        val query = buildQuery("SELECT * FROM $tableName LIMIT ?, ?", offset, pageSize)
        return Pagination(doGetAll(query), count())
    }

    suspend fun deleteAll(): Boolean {
        val query = buildQuery("DELETE FROM $tableName")
        return doScalar(query) > 0
    }

    suspend fun deleteById(id: Long): Boolean {
        val query = buildQuery("DELETE FROM $tableName WHERE id = ?", id)
        return doScalar(query) > 0
    }

    suspend fun deleteByIds(ids: List<Number>): Boolean {
        if (ids.isNotEmpty()) {
            val placeholders = ids.joinToString(", ") { "?" }
            val query = buildQuery(
                "DELETE FROM $tableName WHERE id IN ($placeholders)", *ids.toTypedArray()
            )
            return doScalar(query) > 0
        }
        return false
    }

    suspend fun deleteAllBeforeId(id: Long): Boolean {
        val query = buildQuery("DELETE FROM $tableName WHERE id <= ?", id)
        return doScalar(query) > 0
    }

    suspend fun delete(item: T): Boolean {
        return doDelete(item) > 0
    }

    suspend fun delete(items: List<T>): Boolean {
        return doDelete(items) > 0
    }

    data class Pagination<T>(val items: List<T>, val totalItems: Long)
}