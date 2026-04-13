package com.example.gym.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gym.R;
import com.example.gym.data.entity.TrainingDay;

import java.util.List;

public class TrainingDayAdapter extends RecyclerView.Adapter<TrainingDayAdapter.ViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(TrainingDay day);
    }

    private final List<TrainingDay> days;
    private final OnDayClickListener listener;

    public TrainingDayAdapter(List<TrainingDay> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workouts, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingDay day = days.get(position);
        holder.name.setText(day.name);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.training_day_name);
        }
    }
}
