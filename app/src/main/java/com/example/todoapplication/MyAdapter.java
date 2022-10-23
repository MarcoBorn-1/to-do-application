package com.example.todoapplication;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.todoapplication.R;
import com.example.todoapplication.Task;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final ArrayList<Task> tasksSetList;
    private static MyRecyclerViewClickListener myListener;
    private final HashMap<Integer, String> categoryList;

    MyAdapter(ArrayList<Task> tasksList, HashMap<Integer, String> categoryArrayList, MyRecyclerViewClickListener listener){
        tasksSetList = tasksList;
        myListener = listener;
        categoryList = categoryArrayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v;
        v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview, viewGroup, false);
        return new TasksViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewholder, int position) {
        TasksViewHolder holder = (TasksViewHolder) viewholder;
        holder.title.setText(tasksSetList.get(position).getTitle());
        holder.content.setText(tasksSetList.get(position).getDescription());
        holder.check.setChecked(tasksSetList.get(position).getIs_finished());
        holder.dateReg.setText(tasksSetList.get(position).getDate_of_deadline());
        if (tasksSetList.get(position).getIs_finished()) {
            holder.dateDone.setText(tasksSetList.get(position).getDate_of_finish());
            holder.cv.setCardBackgroundColor(Color.LTGRAY);
            holder.dateDone.setVisibility(View.VISIBLE);
            holder.dateDoneTitle.setVisibility(View.VISIBLE);
        }
        else {
            holder.dateDone.setText("");
            holder.dateDone.setVisibility(View.GONE);
            holder.dateDoneTitle.setVisibility(View.GONE);
            holder.cv.setCardBackgroundColor(Color.WHITE);
        }

        if (tasksSetList.get(position).getCategory() == -1) {
            holder.category.setText("Brak kategorii");
        }
        else {
            holder.category.setText(categoryList.get(tasksSetList.get(position).getCategory()));
        }

        if (tasksSetList.get(position).getAttachment() == null) {
            holder.attachmentIcon.setVisibility(View.GONE);
        }
        else {
            holder.attachmentIcon.setVisibility(View.VISIBLE);
        }

        if (!tasksSetList.get(position).getNotification()) {
            holder.notificationIcon.setVisibility(View.GONE);
        }
        else {
            holder.notificationIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public int getItemCount() {
        return tasksSetList.size();
    }


    public interface MyRecyclerViewClickListener
    {
        void onClick (View v, int position);
    }

    public static class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        CardView cv;
        TextView title;
        TextView content;
        CheckBox check;
        TextView dateReg;
        TextView dateDoneTitle;
        TextView dateDone;
        TextView category;
        ImageView notificationIcon;
        ImageView attachmentIcon;

        TasksViewHolder(View convertView) {
            super(convertView);
            cv = itemView.findViewById(R.id.cv);
            title = convertView.findViewById(R.id.task_title);
            content = convertView.findViewById(R.id.content);
            check = convertView.findViewById(R.id.check);
            dateReg = convertView.findViewById(R.id.date2);
            dateDone = convertView.findViewById(R.id.done2);
            category = convertView.findViewById(R.id.task_category);
            notificationIcon = convertView.findViewById(R.id.notificationIcon);
            attachmentIcon = convertView.findViewById(R.id.attachmentIcon);
            dateDoneTitle = convertView.findViewById(R.id.done1);
            cv.setOnClickListener(this);
            check.setOnClickListener(this);
            notificationIcon.setOnClickListener(this);
            attachmentIcon.setOnClickListener(this);

        }
        @Override
        public void onClick(View v) {
            myListener.onClick(v, getAdapterPosition());
        }
    }
}