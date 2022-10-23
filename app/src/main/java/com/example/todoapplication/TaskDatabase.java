package com.example.todoapplication;

import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_TITLE;

import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public final class TaskDatabase {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private TaskDatabase() {}

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "task_list";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DESC = "description";
        public static final String COLUMN_NAME_CATEGORY = "category";
        public static final String COLUMN_NAME_DATE_OF_CREATION = "date_of_creation";
        public static final String COLUMN_NAME_DATE_OF_DEADLINE = "date_of_deadline";
        public static final String COLUMN_NAME_DATE_OF_FINISH = "date_of_finish";
        public static final String COLUMN_NAME_IS_FINISHED = "is_finished";
        public static final String COLUMN_NAME_ATTACHMENT = "attachment";
        public static final String COLUMN_NAME_HAS_NOTIFICATIONS = "notifications";
    }

    public static String[] getAllColumns() {
        String[] columns = new String[10];
        columns[0] = BaseColumns._ID;
        columns[1] = (FeedEntry.COLUMN_NAME_TITLE);
        columns[2] = (FeedEntry.COLUMN_NAME_DESC);
        columns[3] = (FeedEntry.COLUMN_NAME_CATEGORY);
        columns[4] = (FeedEntry.COLUMN_NAME_DATE_OF_CREATION);
        columns[5] = (FeedEntry.COLUMN_NAME_DATE_OF_DEADLINE);
        columns[6] = (FeedEntry.COLUMN_NAME_DATE_OF_FINISH);
        columns[7] = (FeedEntry.COLUMN_NAME_IS_FINISHED);
        columns[8] = (FeedEntry.COLUMN_NAME_ATTACHMENT);
        columns[9] = (FeedEntry.COLUMN_NAME_HAS_NOTIFICATIONS);
        return columns;
    }
}