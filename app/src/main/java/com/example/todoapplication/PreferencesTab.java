package com.example.todoapplication;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static java.security.AccessController.getContext;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PreferencesTab extends AppCompatActivity {
    ArrayList<String> categoryArray = new ArrayList<>();
    ArrayList<Task> taskList;
    HashMap<Integer, String> categoryHashMap = new HashMap<>();
    ArrayList<String> timeArray = new ArrayList<>();
    Spinner categorySpinner;
    Spinner timeSpinner;
    CheckBox hideTaskCheckbox;
    CheckBox showSingleCategoryCheckbox;
    Button addCategory;
    Button removeCategory;

    boolean hideDoneTasks;
    boolean showSingleCategory;
    String selectedCategory;
    int notificationDelay;

    int time;
    int choice = -1;
    String categoryToDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        timeArray.add("1 min");
        timeArray.add("5 min");
        timeArray.add("10 min");
        timeArray.add("15 min");
        timeArray.add("30 min");
        timeArray.add("60 min");

        taskList = new ArrayList<>();

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("SHARED_PREF", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        hideDoneTasks = sharedPreferences.getBoolean("hide-done-tasks", false);
        showSingleCategory = sharedPreferences.getBoolean("show-single-category", false);
        selectedCategory = sharedPreferences.getString("single-category", null);
        notificationDelay = sharedPreferences.getInt("notification-delay", 5);

        getCategoryArray();
        ArrayAdapter<String> categoryArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryArray);
        categoryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        categorySpinner = findViewById(R.id.categorySpinner);
        categorySpinner.setEnabled(sharedPreferences.getBoolean("show-single-category", false));
        timeSpinner = findViewById(R.id.timeSpinner);
        hideTaskCheckbox = findViewById(R.id.hideTaskCheckbox);
        hideTaskCheckbox.setChecked(sharedPreferences.getBoolean("hide-done-tasks", false));
        hideTaskCheckbox.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();
            editor.putBoolean("hide-done-tasks", checked);
            editor.apply();
        });
        showSingleCategoryCheckbox = findViewById(R.id.showSingleCategoryCheckbox);
        showSingleCategoryCheckbox.setChecked(sharedPreferences.getBoolean("show-single-category", false));
        showSingleCategoryCheckbox.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();
            if (checked) {
                categorySpinner.setEnabled(true);
                if (categoryArray.size() != 0) {
                    editor.putBoolean("show-single-category", true);
                    editor.putString("single-category", categoryArray.get(0));
                    showSingleCategory = true;
                }
            } else {
                categorySpinner.setEnabled(false);
                editor.putBoolean("show-single-category", false);
                editor.putString("single-category", null);
                showSingleCategory = false;
            }
            editor.apply();
        });
        addCategory = findViewById(R.id.addCategoryButton);
        removeCategory = findViewById(R.id.removeCategoryButton);

        addCategory.setOnClickListener(v -> addNewCategoryDialog());
        removeCategory.setOnClickListener(v -> removeCategoryDialog());

        // Category spinner setup


        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextSize(40);
                selectedCategory = parent.getItemAtPosition(position).toString();
                if (showSingleCategory) {
                    editor.putString("single-category", selectedCategory);
                    editor.apply();
                }

            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        categorySpinner.setAdapter(categoryArrayAdapter);
        categorySpinner.setSelection((categoryArray.contains(selectedCategory) ? categoryArray.indexOf(selectedCategory) : 0), false);

        // Time adapter setup

        ArrayAdapter<String> timeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, timeArray);
        timeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextSize(40);
                time = Integer.parseInt(parent.getItemAtPosition(position).toString().split(" ")[0]);
                if (time != notificationDelay) {
                    notificationDelay = time;
                    editor.putInt("notification-delay", time);
                    editor.apply();

                    getTasks();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    Calendar currentDate = Calendar.getInstance();
                    for (Task task: taskList) {
                        Calendar date = Calendar.getInstance();
                        try {
                            date.setTime(Objects.requireNonNull(sdf.parse(task.getDate_of_deadline())));
                            String currentTimeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date.getTime());
                            if (date.getTimeInMillis() - currentDate.getTimeInMillis() - (60L * notificationDelay * 1000) > 0) {
                                Intent intent = new Intent(PreferencesTab.this, ReminderBroadcast.class);
                                intent.putExtra("id", taskList.get(position).getId());
                                intent.putExtra("title", taskList.get(position).getTitle());
                                intent.putExtra("date", currentTimeStamp);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(PreferencesTab.this, task.getId(), intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                                long notificationTime = date.getTimeInMillis() - (60L * notificationDelay * 1000);
                                System.out.println("Notification set!" + task.getId());
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
            }
        });
        timeSpinner.setAdapter(timeArrayAdapter);
        int selection = (timeArray.contains(notificationDelay + " min") ? timeArray.indexOf(notificationDelay + " min") : 0);
        timeSpinner.setSelection(selection, false);
    }

    private void getTasks() {
        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_read = dbHelperTask.getReadableDatabase();
        Cursor cursor;
        // get all tasks
        cursor = db_read.query(false, TaskDatabase.FeedEntry.TABLE_NAME, TaskDatabase.getAllColumns(), null, null, null, null, null, null);

        if (!taskList.isEmpty()) {
            taskList.clear();
        }
        while(cursor.moveToNext()) {
            Task task = new Task();
            task.setId(cursor.getInt(0));
            task.setTitle(cursor.getString(1));
            task.setDescription(cursor.getString(2));
            task.setCategory(cursor.getInt(3));
            task.setDate_of_creation(cursor.getString(4));
            task.setDate_of_deadline(cursor.getString(5));
            task.setDate_of_finish(cursor.getString(6));
            task.setIs_finished(cursor.getInt(7));
            task.setAttachment(cursor.getString(8));
            task.setNotification(cursor.getInt(9));
            if (task.getNotification()) taskList.add(task);
        }
        Collections.sort(taskList);
        cursor.close();
    }

    public void getCategoryArray() {
        categoryArray = new ArrayList<>();
        CategoryDatabaseHelper dbHelper = new CategoryDatabaseHelper(this);
        SQLiteDatabase db_read = dbHelper.getReadableDatabase();
        Cursor cursor = db_read.query(false, CategoryDatabase.FeedEntry.TABLE_NAME, CategoryDatabase.getAllColumns(), null, null, null, null, null, null);
        while(cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String categoryName = cursor.getString(1);
            categoryArray.add(categoryName);
            categoryHashMap.put(id, categoryName);
        }
        cursor.close();
    }

    public void addNewCategoryDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Podaj nową kategorię");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addNewCategory(input.getText().toString());
            }
        });

        builder.setView(input);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void addNewCategory(String category) {
        if (categoryArray != null) {
            if (categoryArray.size() == 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Maksimum dla ilości kategorii został osiągnięty! (10)");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return;
            }
        }
        CategoryDatabaseHelper dbHelperCat = new CategoryDatabaseHelper(this);
        SQLiteDatabase db_cat_write = dbHelperCat.getWritableDatabase();
        SQLiteDatabase db_cat_read = dbHelperCat.getReadableDatabase();

        // Sprawdzamy, czy taka kategoria czasem już nie istnieje

        Cursor check_if_exists = db_cat_read.rawQuery("SELECT *" + " FROM " + CategoryDatabase.FeedEntry.TABLE_NAME + " WHERE " + CategoryDatabase.FeedEntry.COLUMN_NAME_CATEGORY_NAME + " LIKE '%" + category + "%'", null);
        if (check_if_exists.moveToFirst()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Taka kategoria już istnieje!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            check_if_exists.close();
            return;
        }

        // Dodajemy kategorię

        ContentValues newCategory = new ContentValues();
        newCategory.put(CategoryDatabase.FeedEntry.COLUMN_NAME_CATEGORY_NAME, category);
        db_cat_write.insert(CategoryDatabase.FeedEntry.TABLE_NAME, null, newCategory);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodano nową kategorię!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setOnDismissListener(v -> {
            finish();
            startActivity(getIntent());
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void removeCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (categoryArray.size() > 0) {
            builder.setTitle("Wybierz kategorię:");

            // add a radio button list
            String[] options = categoryArray.toArray(new String[0]);
            int checkedItem = -1;
            builder.setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice = which;
                    categoryToDelete = options[which];
                }
            });

            // add OK and Cancel buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (choice == -1) return;
                    removeCategory();
                }
            });
            builder.setNegativeButton("Cancel", null);

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            builder.setTitle("Brak kategorii do wyboru!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void removeCategory() {
        CategoryDatabaseHelper dbHelperCat = new CategoryDatabaseHelper(this);
        SQLiteDatabase db_cat_write = dbHelperCat.getWritableDatabase();

        // Removing category from Category table in database

        String selection1 = CategoryDatabase.FeedEntry.COLUMN_NAME_CATEGORY_NAME + " LIKE ?";
        String[] selectionArgs1 = { categoryToDelete };
        int deletedRows = db_cat_write.delete(CategoryDatabase.FeedEntry.TABLE_NAME, selection1, selectionArgs1);

        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db = dbHelperTask.getWritableDatabase();

        // New value for one column
        int newValue = -1;
        ContentValues values = new ContentValues();
        values.put(TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY, newValue);

        // Which row to update, based on the title
        String selection2 = TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY + " LIKE ?";
        String[] selectionArgs2 = { Objects.requireNonNull(getKeyByValue(categoryHashMap, categoryToDelete)).toString() };

        int count = db.update(
                TaskDatabase.FeedEntry.TABLE_NAME,
                values,
                selection2,
                selectionArgs2);

        System.out.println("Updated " + count + "tasks!");

        if (deletedRows > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Usunięto kategorię " + categoryToDelete + "!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.setOnDismissListener(v -> {
                finish();
                startActivity(getIntent());
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}