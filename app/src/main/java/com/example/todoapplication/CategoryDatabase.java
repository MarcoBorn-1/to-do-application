package com.example.todoapplication;

import android.provider.BaseColumns;

public class CategoryDatabase {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private CategoryDatabase() {}

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "category_list";
        public static final String COLUMN_NAME_CATEGORY_NAME = "name";
    }

    public static String[] getAllColumns() {
        String[] columns = new String[2];
        columns[0] = BaseColumns._ID;
        columns[1] = (CategoryDatabase.FeedEntry.COLUMN_NAME_CATEGORY_NAME);

        return columns;
    }
}
