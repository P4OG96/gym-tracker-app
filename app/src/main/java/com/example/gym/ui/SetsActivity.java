package com.example.gym.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gym.R;
import com.example.gym.adapter.SetAdapter;
import com.example.gym.data.database.AppDatabase;
import com.example.gym.data.entity.Exercise;
import com.example.gym.data.entity.ExerciseSet;

import java.util.List;



public class SetsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "timer_prefs";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_IS_RUNNING = "is_running";
    private static final int MAX_SECONDS = 300; // 5 Minuten
    private List<ExerciseSet> sets;
    private SetAdapter adapter;
    private Button timerButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int seconds = 0;
    private boolean isRunning = false;
    private long startTimeMillis = 0; // Zeitpunkt, an dem Timer gestartet wurde

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        int exerciseId = getIntent().getIntExtra("exerciseId", -1);

        Exercise exercise = AppDatabase.getInstance(this).exerciseDao().getById(exerciseId);

        TextView currentExerciseText = findViewById(R.id.current_exercise);
        if (exercise != null) {
            currentExerciseText.setText(exercise.name);
        } else {
            currentExerciseText.setText("Übung nicht gefunden");
        }

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sets = AppDatabase.getInstance(this).exerciseSetDao().getForExercise(exerciseId);
        adapter = new SetAdapter(sets, this);
        recyclerView.setAdapter(adapter);

        // Timer Button
        timerButton = findViewById(R.id.timer_button);
        timerButton.setText("00:00");

        timerButton.setOnLongClickListener(v -> {
            long now = System.currentTimeMillis();
            if (!isRunning) {
                isRunning = true;
                startTimeMillis = now;

                // In SharedPreferences speichern
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putBoolean(KEY_IS_RUNNING, true)
                        .putLong(KEY_START_TIME, startTimeMillis)
                        .apply();

                handler.post(runnable);
            } else {
                // Reset
                startTimeMillis = now;
                seconds = 0;
                updateTimerButton();

                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(KEY_START_TIME, startTimeMillis)
                        .apply();
            }
            return true;
        });

        // Add / Remove Set Buttons
        Button addSetBtn = findViewById(R.id.add_set);
        addSetBtn.setOnClickListener(v -> {
            ExerciseSet newSet = new ExerciseSet();
            newSet.exerciseId = exerciseId;
            newSet.setNumber = sets.size() + 1;
            newSet.weight = 0;
            newSet.reps = 0;
            newSet.completed = false;

            long id = AppDatabase.getInstance(this).exerciseSetDao().insert(newSet);
            newSet.id = (int) id;
            sets.add(newSet);
            adapter.notifyItemInserted(sets.size() - 1);
        });

        Button removeSetBtn = findViewById(R.id.sub_set);
        removeSetBtn.setOnClickListener(v -> {
            if (!sets.isEmpty()) {
                int lastIndex = sets.size() - 1;
                ExerciseSet lastSet = sets.get(lastIndex);
                boolean hasValues = lastSet.weight > 0 || lastSet.reps > 0;

                if (hasValues) {
                    new AlertDialog.Builder(this)
                            .setTitle("Set löschen")
                            .setMessage("Dieses Set enthält bereits Daten. Wirklich löschen?")
                            .setPositiveButton("Ja", (dialog, which) -> deleteSet(lastIndex, lastSet))
                            .setNegativeButton("Nein", null)
                            .show();
                } else {
                    deleteSet(lastIndex, lastSet);
                }
            }
        });

        if (savedInstanceState != null) {
            isRunning = savedInstanceState.getBoolean("isRunning", false);
            startTimeMillis = savedInstanceState.getLong("startTimeMillis", 0);

            if (isRunning) {
                handler.post(runnable);
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isRunning = prefs.getBoolean(KEY_IS_RUNNING, false);
        startTimeMillis = prefs.getLong(KEY_START_TIME, 0);

        if (isRunning) {
            handler.post(runnable);
        }
    }
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                updateTimerButton();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void deleteSet(int index, ExerciseSet set) {
        AppDatabase.getInstance(this).exerciseSetDao().delete(set);
        sets.remove(index);

        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).setNumber = i + 1;
            AppDatabase.getInstance(this).exerciseSetDao().update(sets.get(i));
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isRunning = savedInstanceState.getBoolean("isRunning", false);
        startTimeMillis = savedInstanceState.getLong("startTimeMillis", 0);

        if (isRunning) {
            handler.post(runnable);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isRunning", isRunning);
        outState.putLong("startTimeMillis", startTimeMillis);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable); // Timer stoppen
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) {
            updateTimerButton();  // Button sofort anzeigen
            handler.post(runnable); // Runnable weiterlaufen lassen
        }
    }
    private void updateTimerButton() {
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        seconds = (int) (elapsedMillis / 1000);

        if (seconds >= MAX_SECONDS) {
            seconds = MAX_SECONDS;
            isRunning = false;
            handler.removeCallbacks(runnable);
        }

        int minutes = seconds / 60;
        int secs = seconds % 60;

        timerButton.setText(String.format("%d:%02d", minutes, secs));
    }
}