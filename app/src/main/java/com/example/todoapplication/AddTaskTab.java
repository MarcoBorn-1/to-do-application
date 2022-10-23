package com.example.todoapplication;

import static com.example.todoapplication.TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class AddTaskTab extends AppCompatActivity {
    private static final int RESULT_LOAD_IMAGE = 1;
    HashMap<Integer, String> categoryHashMap;
    ArrayList<String> categoryList;
    String[] categoryArray;
    String[] options;
    TextInputEditText titleInput;
    TextInputEditText descInput;
    ActivityResultLauncher<String> activityResultLauncher;
    Uri photoURI;
    Calendar date;

    TextView deadlineText;
    TextView categoryText;
    TextView attachmentText;
    Button deadlineButton;
    Button categoryButton;
    Button addTaskButton;
    Button addFileButton;
    CheckBox notificationCheckbox;

    String attachmentString;
    int choice = 0;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    int notificationDelay;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        createNotificationChannel();
        sharedPreferences = getApplicationContext().getSharedPreferences("SHARED_PREF", 0);
        editor = sharedPreferences.edit();

        notificationDelay = sharedPreferences.getInt("notification-delay", 5);

        boolean isLoaded = loadCategories();
        if (isLoaded) {
            categoryArray = categoryList.toArray(new String[0]);
        }
        else {
            categoryArray = null;
        }

        attachmentString = null;
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {

                if (result == null) {
                    attachmentString = null;
                    addFileButton.setText("Dodaj załącznik");
                    attachmentText.setText("Brak załącznika");
                }
                else {
                    photoURI = result;
                    attachmentString = getFileName(result);

                    File f = new File(getFilesDir(), getFileName(result));
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        inputStream = getContentResolver().openInputStream(result);
                        outputStream = new FileOutputStream(f);
                        byte[] array = new byte[1000000];
                        int len;
                        while ((len = inputStream.read(array)) > 0) {
                            outputStream.write(array, 0, len);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            assert inputStream != null;
                            inputStream.close();
                            assert outputStream != null;
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    addFileButton.setText("Zmień załącznik");
                    attachmentText.setText("Dodano załącznik");
                }
            }
        });

        deadlineButton = findViewById(R.id.datetimeButton);
        categoryButton = findViewById(R.id.categoryButton);
        addTaskButton = findViewById(R.id.addTaskButton);
        addFileButton = findViewById(R.id.addFileButton);
        deadlineText = findViewById(R.id.deadlineText);
        categoryText = findViewById(R.id.categoryText);
        attachmentText = findViewById(R.id.attachmentText);

        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descriptionInput);
        notificationCheckbox = findViewById(R.id.notificationCheckbox);

        deadlineButton.setOnClickListener(v -> showDateTimeDialog());
        categoryButton.setOnClickListener(v -> showCategoryDialog(isLoaded));
        addTaskButton.setOnClickListener(v -> addTask());
        addFileButton.setOnClickListener(v -> addFile());

    }


    private void addFile() {
        activityResultLauncher.launch("*/*");
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri != null) {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int c = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (c >= 0)
                            result = cursor.getString(c);
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private boolean loadCategories() {
        categoryList = new ArrayList<>();
        categoryHashMap = new HashMap<>();
        CategoryDatabaseHelper dbHelperCat = new CategoryDatabaseHelper(this);
        SQLiteDatabase db_category_read = dbHelperCat.getReadableDatabase();
        Cursor cursor = db_category_read.query(false, CategoryDatabase.FeedEntry.TABLE_NAME,
                CategoryDatabase.getAllColumns(), null,
                null, null, null, null, null);

        while(cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String category = cursor.getString(1);
            categoryList.add(category);
            categoryHashMap.put(id, category);
        }
        cursor.close();

        return categoryList.size() != 0;
    }

    private void showCategoryDialog(boolean category_present) {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (category_present) {
            builder.setTitle("Wybierz kategorię:");

            // add a radio button list
            options = categoryArray;
            int checkedItem = -1;
            builder.setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    choice = which;
                    System.out.println(which);
                }
            });

            // add OK and Cancel buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    categoryText.setText(options[choice]);
                    categoryButton.setText("Zmień kategorię");
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

    private void showDateTimeDialog() {
        final Calendar calendar=Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);

                TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY,hourOfDay);
                        calendar.set(Calendar.MINUTE,minute);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                        date = calendar;
                        deadlineText.setText(simpleDateFormat.format(calendar.getTime()));
                        deadlineButton.setText("Zmień datę/czas");
                    }
                };
                new TimePickerDialog(AddTaskTab.this,timeSetListener,calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),true).show();
            }
        };
        new DatePickerDialog(AddTaskTab.this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void addTask() {
        String title = (titleInput.getText().toString().equals("") ? null : titleInput.getText().toString());
        String description = (descInput.getText().toString().equals("") ? null : descInput.getText().toString());
        String deadline = (deadlineText.getText().toString().equals("Deadline") ? null : deadlineText.getText().toString());
        String category = (categoryText.getText().toString().equals("Kategoria") ? null : categoryText.getText().toString());
        String attachment = (attachmentText.getText().toString().equals("Brak załącznika") ? null : attachmentString);
        boolean hasNotifications = notificationCheckbox.isChecked();

        if (title == null || description == null || deadline == null || category == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Wypełnij wszystkie wymagane pola! (tytuł, opis, deadline, kategoria)");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        }
        Calendar calendar = Calendar.getInstance();
        String currentTimeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());

        TaskDatabaseHelper dbHelperTask = new TaskDatabaseHelper(this);
        SQLiteDatabase db_task_write = dbHelperTask.getWritableDatabase();
        ContentValues newTask = new ContentValues();
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_TITLE, title);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_DESC, description);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_CATEGORY, getKeyByValue(categoryHashMap, options[choice]));
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_CREATION, currentTimeStamp);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_DEADLINE, deadline);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_DATE_OF_FINISH, "");
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_IS_FINISHED, 0);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_ATTACHMENT, attachment);
        newTask.put(TaskDatabase.FeedEntry.COLUMN_NAME_HAS_NOTIFICATIONS, (notificationCheckbox.isChecked() ? 1 : 0));
        long newRowId = db_task_write.insert(TaskDatabase.FeedEntry.TABLE_NAME, null, newTask);

        if (hasNotifications){
            Intent intent = new Intent(this, ReminderBroadcast.class);
            intent.putExtra("id", (int) newRowId);
            intent.putExtra("title", title);
            intent.putExtra("date", deadline);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) newRowId, intent, PendingIntent.FLAG_MUTABLE);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            long notificationTime = date.getTimeInMillis() - (60L * notificationDelay * 1000);
            if (notificationTime - calendar.getTimeInMillis() > 0) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dodano nowe zadanie!");
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

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String getPath(Uri uri) {

        String path = null;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if(cursor == null){
            path = uri.getPath();
        }
        else{
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
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
}