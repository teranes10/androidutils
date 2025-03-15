package com.github.teranes10.androidutils.models

data class Pagination<T>(
    val items: List<T>,
    val totalItems: Int
)