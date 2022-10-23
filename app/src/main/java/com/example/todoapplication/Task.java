package com.example.todoapplication;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class Task implements Comparable<Task>{
    public int id;
    public String title;
    public String description;
    public Integer category;
    public String date_of_creation;
    public String date_of_deadline;
    public String date_of_finish;
    public int is_finished;
    public String attachment;
    public int notification;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public String getDate_of_creation() {
        return date_of_creation;
    }

    public void setDate_of_creation(String date_of_creation) {
        this.date_of_creation = date_of_creation;
    }

    public String getDate_of_deadline() {
        return date_of_deadline;
    }

    public void setDate_of_deadline(String date_of_deadline) {
        this.date_of_deadline = date_of_deadline;
    }

    public String getDate_of_finish() {
        return date_of_finish;
    }

    public void setDate_of_finish(String date_of_finish) {
        this.date_of_finish = date_of_finish;
    }

    public boolean getIs_finished() {
        return is_finished == 1;
    }

    public void setIs_finished(int is_finished) {
        this.is_finished = is_finished;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public boolean getNotification() {
        return notification == 1;
    }

    public void setNotification(int notification) {
        this.notification = notification;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", category=" + category +
                ", date_of_creation='" + date_of_creation + '\'' +
                ", date_of_deadline='" + date_of_deadline + '\'' +
                ", date_of_finish='" + date_of_finish + '\'' +
                ", is_finished=" + is_finished +
                ", attachment='" + attachment + '\'' +
                ", notification='" + notification + '\'' +
                '}';
    }

    @Override
    public int compareTo(Task o) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Calendar dateOfDeadline1 = Calendar.getInstance();
        Calendar dateOfDeadline2 = Calendar.getInstance();
        try {
            dateOfDeadline1.setTime(Objects.requireNonNull(formatter.parse(getDate_of_deadline())));
        }
        catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }

        try {
            dateOfDeadline2.setTime(Objects.requireNonNull(formatter.parse(o.getDate_of_deadline())));
        }
        catch (ParseException e) {
            e.printStackTrace();
            return 1;
        }

        try {
            return (int) ((dateOfDeadline1.getTimeInMillis() / 1000) - (dateOfDeadline2.getTimeInMillis()/1000));
        }
        catch (NullPointerException e) {
            return 0;
        }
    }
}
