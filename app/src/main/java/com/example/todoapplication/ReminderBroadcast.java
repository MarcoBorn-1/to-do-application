package com.example.todoapplication;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderBroadcast extends BroadcastReceiver {
    Integer id = 1000;
    String reminderTitle = "Empty title";
    String reminderDeadline = "error";
    @Override
    public void onReceive(Context context, Intent intent) {
        id = intent.getIntExtra("id", 1000);
        reminderTitle = intent.getStringExtra("title");
        reminderDeadline = intent.getStringExtra("date");

        Intent newIntent = new Intent(context, MainActivity.class);
        newIntent.putExtra("id", id);
        newIntent.setAction(Long.toString(System.currentTimeMillis()));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, newIntent, PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "todo")
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(reminderTitle)
                .setContentText("Przypomnienie")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Przypominam o zadaniu, planowane wykonanie " + reminderDeadline))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(id, builder.build());
    }
}
