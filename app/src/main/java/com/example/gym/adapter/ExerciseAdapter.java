package com.example.gym.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gym.R;
import com.example.gym.data.database.AppDatabase;
import com.example.gym.data.entity.Exercise;
import com.example.gym.ui.SetsActivity;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private Context context;
    private Runnable onCompletedChanged;

    public ExerciseAdapter(List<Exercise> exercises, Context context, Runnable onCompletedChanged) {
        this.exercises = exercises;
        this.context = context;
        this.onCompletedChanged = onCompletedChanged;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exercises, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.name.setText(exercise.name);

        if (exercise.completed) {
            holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.name.setPaintFlags(holder.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(exercise.completed);
        holder.checkBox.setOnCheckedChangeListener((b, checked) -> {
            exercise.completed = checked;
            AppDatabase.getInstance(context).exerciseDao().update(exercise);

            if (checked) {
                holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.name.setPaintFlags(holder.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
            
            if (onCompletedChanged != null) onCompletedChanged.run();
        });

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            Exercise clicked = exercises.get(pos);

            // Prüfen, ob Exercise in DB existiert
            Exercise dbExercise = AppDatabase.getInstance(context).exerciseDao().getById(clicked.id);
            if (dbExercise == null) {
                Toast.makeText(context, "Diese Übung existiert nicht mehr!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(context, SetsActivity.class);
            intent.putExtra("exerciseId", clicked.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }
}
