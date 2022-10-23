package com.example.todoapplication;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_ATTACHMENT;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_CREATION;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_DEADLINE;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_FINISH;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_DESC;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_HAS_NOTIFICATIONS;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_IS_FINISHED;
import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_TITLE;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements MyAdapter.MyRecyclerViewClickListener {

    ActivityResultLauncher<String> activityResultLauncher;
    ArrayList<Task> taskList = null;
    ArrayList<String> categoryArrayList;
    String[] categoryStringArray;
    HashMap<Integer, String> categoryHashMap;
    SearchView searchView;
    RecyclerView recyclerView;
    FloatingActionButton actionButton;
    ImageButton imageButton;
    RecyclerView.Adapter adapter;
    AlarmManager alarmManager;

    boolean hideDoneTasks;
    boolean showSingleCategory;
    String singleCategory;
    int notificationDelay;

    int choice = -1;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (taskList == null) taskList = new ArrayList<>();

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("SHARED_PREF", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        hideDoneTasks = sharedPreferences.getBoolean("hide-done-tasks", false);
        showSingleCategory = sharedPreferences.getBoolean("show-single-category", false);
        singleCategory = sharedPreferences.getString("single-category", null);
        notificationDelay = sharedPreferences.getInt("notification-delay", 1);

        editor.apply();


        getCategories();
        getTasks(null, (showSingleCategory ? singleCategory : null), hideDoneTasks);


        Integer notification_id = getIntent().getIntExtra("id", -727);
        if (notification_id != -727) {
            TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
            SQLiteDatabase db_read = dbHelperTask.getReadableDatabase();
            String selection = TaskDatabase.FeedEntry._ID + " = ?";
            String[] selectionArgs = { notification_id.toString() };
            Cursor cursor = db_read.query(false, TaskDatabase.FeedEntry.TABLE_NAME, TaskDatabase.getAllColumns(), selection, selectionArgs, null, null, null, null);
            Task task = new Task();
            if (cursor.moveToFirst()) {
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
                createTaskDialog(task);
            }
        }

        imageButton = findViewById(R.id.imageButton);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MyAdapter(taskList, categoryHashMap, this);
        recyclerView.setAdapter(adapter);

        imageButton.setOnClickListener(this::openPreferencesActivity);
        actionButton = findViewById(R.id.floatingButton);
        searchView = findViewById(R.id.searchView);
        searchView.setIconifiedByDefault(true);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // Override onQueryTextSubmit method
            // which is call
            // when submitquery is searched

            @Override
            public boolean onQueryTextSubmit(String query)
            {
                if (!query.isEmpty()) {
                    getTasks(query, (showSingleCategory ? singleCategory : null), hideDoneTasks);
                    adapter.notifyDataSetChanged();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                if (newText.equals("")) {
                    getTasks(null, (showSingleCategory ? singleCategory : null), hideDoneTasks);
                    adapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("SHARED_PREF", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        hideDoneTasks = sharedPreferences.getBoolean("hide-done-tasks", false);
        showSingleCategory = sharedPreferences.getBoolean("show-single-category", false);
        singleCategory = sharedPreferences.getString("single-category", null);
        notificationDelay = sharedPreferences.getInt("notification-delay", 1);

        editor.apply();

        getCategories();
        getTasks(null, (showSingleCategory ? singleCategory : null), hideDoneTasks);
        adapter.notifyDataSetChanged();
    }

    private void getTasks(String taskSearch, String category, boolean onlyFinished) {
        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_read = dbHelperTask.getReadableDatabase();
        Cursor cursor;
        if (taskSearch == null) {
            // get all tasks
            cursor = db_read.query(false, TaskDatabase.FeedEntry.TABLE_NAME, TaskDatabase.getAllColumns(), null, null, null, null, null, null);
        }
        else {
            String selection = TaskDatabase.FeedEntry.COLUMN_NAME_TITLE + " LIKE ?";
            String[] selectionArgs = { ("%" + taskSearch + "%") };
            cursor = db_read.query(false, TaskDatabase.FeedEntry.TABLE_NAME, TaskDatabase.getAllColumns(), selection, selectionArgs, null, null, null, null);
        }
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
            if (category != null && Objects.equals(categoryHashMap.get(task.getCategory()), category)) {
                if (onlyFinished && !task.getIs_finished()) {
                    taskList.add(task);
                }
                else if (!onlyFinished) {
                    taskList.add(task);
                }
            }
            else if (category == null) {
                if (onlyFinished && !task.getIs_finished()) {
                    taskList.add(task);
                }
                else if (!onlyFinished) {
                    taskList.add(task);
                }
            }

        }
        Collections.sort(taskList);
        cursor.close();
    }

    private void getCategories() {
        CategoryDatabaseHelper dbHelperCat = new CategoryDatabaseHelper(this);
        SQLiteDatabase db_cat_read = dbHelperCat.getReadableDatabase();
        Cursor cursor = db_cat_read.query(false, CategoryDatabase.FeedEntry.TABLE_NAME, CategoryDatabase.getAllColumns(), null, null, null, null, null, null);
        categoryHashMap = new HashMap<>();
        categoryArrayList = new ArrayList<>();
        while(cursor.moveToNext()) {
            categoryHashMap.put(cursor.getInt(0), cursor.getString(1));
            categoryArrayList.add(cursor.getString(1));
        }
        cursor.close();
    }

    public void openAddTaskActivity(View view) {
        Intent intent = new Intent(this, AddTaskTab.class);
        startActivity(intent);
    }

    public void openPreferencesActivity(View view) {
        Intent intent = new Intent(this, PreferencesTab.class);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onClick(View v, int position) {
        switch (v.getId()) {
            case R.id.check:
                if (taskList.get(position).getIs_finished()) {
                    taskList.get(position).setIs_finished(0);
                    taskList.get(position).setDate_of_finish("");
                    setTaskAsDone(false, taskList.get(position).getId(), null);
                  }
                else {
                    Calendar calendar = Calendar.getInstance();
                    String currentTimeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
                    taskList.get(position).setIs_finished(1);
                    taskList.get(position).setDate_of_finish(currentTimeStamp);
                    setTaskAsDone(true, taskList.get(position).getId(), currentTimeStamp);
                    if (hideDoneTasks) {
                        taskList.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
                }
                break;
            case R.id.attachmentIcon:
                File fd = new File(getApplicationContext().getFilesDir(), taskList.get(position).getAttachment());
                String[] fileExtension = taskList.get(position).getAttachment().split("\\.");
                Uri path = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", fd);


                Intent myIntent = new Intent(Intent.ACTION_VIEW);
                //myIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                myIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String type = mime.getMimeTypeFromExtension(fileExtension[1]);
                myIntent.setDataAndType(path, type);

                startActivity(myIntent);
                break;
            case R.id.cv:
                createTaskDialog(taskList.get(position));
                break;
        }
        adapter.notifyItemChanged(position);
    }

    private void deleteTask(Integer id, AlertDialog dialog) {
        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_write = dbHelperTask.getWritableDatabase();

        // Define 'where' part of query.
        String selection = TaskDatabase.FeedEntry._ID + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { id.toString() };
        // Issue SQL statement.
        int deletedRows = db_write.delete(TaskDatabase.FeedEntry.TABLE_NAME, selection, selectionArgs);

        dialog.dismiss();
    }

    private void saveChangesToTask(Task task) {
        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_write = dbHelperTask.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_TITLE, task.getTitle());
        values.put(COLUMN_NAME_DESC, task.getDescription());
        values.put(COLUMN_NAME_CATEGORY, task.getCategory());
        values.put(COLUMN_NAME_DATE_OF_CREATION, task.getDate_of_creation());
        values.put(COLUMN_NAME_DATE_OF_DEADLINE, task.getDate_of_deadline());
        values.put(COLUMN_NAME_DATE_OF_FINISH, task.getDate_of_finish());
        values.put(COLUMN_NAME_IS_FINISHED, (task.getIs_finished() ? 1 : 0));
        values.put(COLUMN_NAME_ATTACHMENT, task.getAttachment());
        values.put(COLUMN_NAME_HAS_NOTIFICATIONS, (task.getNotification() ? 1 : 0));

        String selection = TaskDatabase.FeedEntry._ID + " LIKE ?";
        String[] selectionArgs = { ((Integer) task.getId()).toString() };

        int count = db_write.update(
                TaskDatabase.FeedEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    private void setTaskAsDone(boolean isDone, Integer position, String currentTimeStamp) {

        // Update database on isFinished

        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_write = dbHelperTask.getWritableDatabase();

        // New value for one column
        int status = (isDone ? 1 : 0);
        ContentValues values = new ContentValues();
        values.put(TaskDatabase.FeedEntry.COLUMN_NAME_IS_FINISHED, status);

        // Which row to update, based on the title
        String selection = TaskDatabase.FeedEntry._ID + " LIKE ?";
        String[] selectionArgs = { (position).toString() };

        int count = db_write.update(
                TaskDatabase.FeedEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);


        // Update database on date_finished

        ContentValues values2 = new ContentValues();
        if (isDone) {
            values2.put(TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_FINISH, currentTimeStamp);

            String selection2 = TaskDatabase.FeedEntry._ID + " LIKE ?";
            String[] selectionArgs2 = { (position).toString() };

            int count2 = db_write.update(
                    TaskDatabase.FeedEntry.TABLE_NAME,
                    values2,
                    selection2,
                    selectionArgs2);

        }
        else {
            values2.put(TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_FINISH, "");

            String selection2 = TaskDatabase.FeedEntry._ID + " LIKE ?";
            String[] selectionArgs2 = { (position).toString() };

            int count2 = db_write.update(
                    TaskDatabase.FeedEntry.TABLE_NAME,
                    values2,
                    selection2,
                    selectionArgs2);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TODO notifications";
            String description = "Channel to send to-do reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("todo", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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

    private void createTaskDialog(Task task) {
        int position = taskList.indexOf(task);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_input, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        TextView title = layout.findViewById(R.id.dialogTitleText);
        TextView description = layout.findViewById(R.id.dialogDescriptionText);
        TextView date_of_creation = layout.findViewById(R.id.dialogDateCreationText);
        TextView date_of_deadline = layout.findViewById(R.id.dialogDateDeadlineText);
        TextView date_of_finish = layout.findViewById(R.id.dialogDateFinishText);
        TextView category = layout.findViewById(R.id.dialogCategoryText);
        TextView attachment = layout.findViewById(R.id.dialogAttachmentText);
        TextView notification = layout.findViewById(R.id.dialogNotificationText);
        Button attachmentButton = layout.findViewById(R.id.dialogAttachmentButton);
        Button deleteButton = layout.findViewById(R.id.dialogDeleteButton);
        LinearLayout deadline = layout.findViewById(R.id.dialogDeadlineLinearLayout);

        if (!task.getIs_finished()) {
            deadline.setVisibility(View.GONE);
        }
        else {
            deadline.setVisibility(View.VISIBLE);
        }

        if ((Objects.equals(task.getAttachment(), null))) {
            attachmentButton.setVisibility(View.GONE);
        }
        else {
            attachmentButton.setVisibility(View.VISIBLE);
            attachmentButton.setOnClickListener(view -> {
                File fd2 = new File(getApplicationContext().getFilesDir(), task.getAttachment());
                String[] fileExtension2 = task.getAttachment().split("\\.");
                Uri path2 = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", fd2);


                Intent myIntent2 = new Intent(Intent.ACTION_VIEW);
                myIntent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                MimeTypeMap mime2 = MimeTypeMap.getSingleton();
                String type2 = mime2.getMimeTypeFromExtension(fileExtension2[1]);
                myIntent2.setDataAndType(path2, type2);

                startActivity(myIntent2);
            });
        }

        title.setText(task.getTitle());
        description.setText(task.getDescription());
        date_of_creation.setText(task.getDate_of_creation());
        date_of_deadline.setText(task.getDate_of_deadline());
        date_of_finish.setText(task.getDate_of_finish());
        category.setText((task.getCategory() == -1 ? "Brak kategorii" : categoryHashMap.get(task.getCategory())));
        attachment.setText((Objects.equals(task.getAttachment(), null) ? "Brak załącznika" : task.getAttachment()));
        notification.setText((task.getNotification() ? "Włączone" : "Wyłączone"));

        builder.setView(layout);
        builder.setTitle("Szczegóły zadania / edycja");
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();

        deleteButton.setOnClickListener(view -> {
            if (task.getNotification()) {
                Intent cancelIntent = new Intent(getApplicationContext(), ReminderBroadcast.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, task.getId(), cancelIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);

                alarmManager.cancel(pendingIntent);
            }
            deleteTask(task.getId(), dialog);
            if (position != -1) {
                adapter.notifyItemRemoved(position);
                taskList.remove(position);
            }
        });
        title.setOnClickListener(view -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(task.getTitle());
            AlertDialog.Builder editTitleBuilder = new AlertDialog.Builder(this);
            editTitleBuilder.setTitle("Podaj nowy tytuł:");
            editTitleBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().toString().equals("") && !input.getText().toString().equals(task.getTitle())) {
                        task.setTitle(input.getText().toString());
                        saveChangesToTask(task);
                        title.setText(input.getText().toString());
                        if (position != -1) {
                            adapter.notifyItemChanged(position);
                        }
                    }
                }
            });
            editTitleBuilder.setView(input);

            AlertDialog titleEditDialog = editTitleBuilder.create();
            titleEditDialog.show();
        });
        description.setOnClickListener(view -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(task.getDescription());
            AlertDialog.Builder editDescBuilder = new AlertDialog.Builder(this);
            editDescBuilder.setTitle("Podaj nowy opis:");
            editDescBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().toString().equals("") && !input.getText().toString().equals(task.getDescription())) {
                        task.setDescription(input.getText().toString());
                        saveChangesToTask(task);
                        description.setText(input.getText().toString());
                        if (position != -1) {
                            adapter.notifyItemChanged(position);
                        }
                    }
                }
            });
            editDescBuilder.setView(input);

            AlertDialog descEditDialog = editDescBuilder.create();
            descEditDialog.show();
        });
        category.setOnClickListener(view -> {
            AlertDialog.Builder categoryBuilder = new AlertDialog.Builder(this);

            if (categoryArrayList.size() != 0) {
                categoryBuilder.setTitle("Wybierz kategorię:");

                categoryStringArray = categoryArrayList.toArray(new String[0]);
                int checkedItem = -1;
                categoryBuilder.setSingleChoiceItems(categoryStringArray, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        choice = which;
                    }
                });

                // add OK and Cancel buttons
                categoryBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (choice != -1) {
                            task.setCategory(getKeyByValue(categoryHashMap, categoryStringArray[choice]));
                            saveChangesToTask(task);
                            category.setText(categoryStringArray[choice]);

                            if (showSingleCategory && singleCategory.equals(categoryStringArray[choice])) {
                                if (position != -1) {
                                    adapter.notifyItemRemoved(position);
                                    taskList.remove(position);
                                }

                            }
                            else {
                                if (position != -1) {
                                    adapter.notifyItemChanged(position);
                                }
                            }
                        }
                    }
                });
                categoryBuilder.setNegativeButton("Cancel", null);

                // create and show the alert dialog
                AlertDialog categoryDialog = categoryBuilder.create();
                categoryDialog.show();
            }
            else {
                categoryBuilder.setTitle("Brak kategorii do wyboru!");
                categoryBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog categoryDialog = categoryBuilder.create();
                categoryDialog.show();
            }
        });

        date_of_deadline.setOnClickListener(view -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    calendar.set(Calendar.YEAR,year);
                    calendar.set(Calendar.MONTH,month);
                    calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);

                    TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                            String timestamp = simpleDateFormat.format(calendar.getTime());

                            Calendar currentTime = Calendar.getInstance();

                            // Jeśli zadanie ma włączone powiadomienia, ustawia na nowo przypominajki
                            if (task.getNotification() && (calendar.getTimeInMillis() - currentTime.getTimeInMillis() - (60L * notificationDelay * 1000) > 0)) {
                                Intent intent = new Intent(MainActivity.this, ReminderBroadcast.class);
                                intent.putExtra("id", task.getId());
                                intent.putExtra("title", task.getTitle());
                                intent.putExtra("date", timestamp);
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, task.getId(), intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                                long notificationTime = calendar.getTimeInMillis() - (60L * notificationDelay * 1000);
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);

                                notification.setText("Włączone");
                            }

                            task.setDate_of_deadline(timestamp);
                            date_of_deadline.setText(timestamp);
                            saveChangesToTask(task);
                            if (position != -1) {
                                adapter.notifyItemChanged(position);
                            }

                        }
                    };
                    new TimePickerDialog(MainActivity.this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),true).show();
                }
            };
            new DatePickerDialog(MainActivity.this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();

        });

        notification.setOnClickListener(view -> {
            if (task.getNotification()) {
                task.setNotification(0);
                saveChangesToTask(task);
                Intent cancelIntent = new Intent(getApplicationContext(), ReminderBroadcast.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, task.getId(), cancelIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);

                alarmManager.cancel(pendingIntent);

                notification.setText("Wyłączone");
            }
            else {
                task.setNotification(1);
                saveChangesToTask(task);

                Intent intent = new Intent(this, ReminderBroadcast.class);
                intent.putExtra("id", task.getId());
                intent.putExtra("title", task.getTitle());
                intent.putExtra("date", task.getDate_of_deadline());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getId(), intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                Calendar date = Calendar.getInstance();
                try {
                    date.setTime(sdf.parse(task.getDate_of_deadline()));
                    Calendar calendar = Calendar.getInstance();

                    long notificationTime = date.getTimeInMillis() - (60L * notificationDelay * 1000);
                    if (notificationTime - calendar.getTimeInMillis() > 0) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
                    }

                    notification.setText("Włączone");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if (position != -1) {
                adapter.notifyItemChanged(position);
            }
        });
    }
}