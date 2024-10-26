package com.github.teranes10.androidutils.utils.database;

import java.util.List;

public class Pagination<T> {
    public List<T> items;
    public int totalItems;

    public Pagination(List<T> items, int totalItems) {
        this.items = items;
        this.totalItems = totalItems;
    }
}
