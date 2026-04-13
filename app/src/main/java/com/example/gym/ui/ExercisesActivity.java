package com.example.gym.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gym.R;
import com.example.gym.adapter.ExerciseAdapter;
import com.example.gym.data.database.AppDatabase;
import com.example.gym.data.entity.Exercise;
import com.example.gym.data.entity.ExerciseSet;
import com.example.gym.data.entity.TrainingDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExercisesActivity extends AppCompatActivity {

    private AppDatabase db;
    private List<TrainingDay> days;
    private List<Exercise> exercises;
    private ExerciseAdapter adapter;
    private int trainingDayId;
    private TextView currentDayText;
    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = AppDatabase.getInstance(this);
        days = db.trainingDayDao().getAll();

        if (days.isEmpty()) {
            startActivity(new Intent(this, WorkoutsActivity.class));
            finish();
            return;
        }

        trainingDayId = getIntent().getIntExtra("trainingDayId", -1);
        if (trainingDayId == -1) {
            SharedPreferences prefs = getSharedPreferences("GymPrefs", MODE_PRIVATE);
            trainingDayId = prefs.getInt("last_day_id", -1);
        }

        setContentView(R.layout.activity_exercises);
        currentDayText = findViewById(R.id.current_day_text);
        RecyclerView recyclerView = findViewById(R.id.recycler);

        if (!isValidTrainingDay()) return;

        setupRecyclerView(recyclerView);
        setupActivityResultLauncher();
        setupButtons();
        saveLastTrainingDay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isValidTrainingDay()) return;
        refreshExercises();
    }

    private boolean isValidTrainingDay() {
        TrainingDay currentDay = db.trainingDayDao().getById(trainingDayId);
        if (currentDay == null) {
            finishAffinity();
            return false;
        }
        currentDayText.setText(currentDay.name);
        return true;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        exercises = db.exerciseDao().getForTrainingDay(trainingDayId);
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).originalPosition = i;
        }
        sortExercises();
        adapter = new ExerciseAdapter(exercises, this, this::onExerciseCompletedChanged);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                Collections.swap(exercises, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                Exercise ex = exercises.get(pos);

                if (direction == ItemTouchHelper.RIGHT) {
                    // → Rechts: Löschen
                    AlertDialog dialog = new AlertDialog.Builder(ExercisesActivity.this)
                            .setTitle("Übung löschen")
                            .setMessage("Möchtest du diese Übung wirklich löschen?")
                            .setPositiveButton("Ja", (d, which) -> {
                                db.exerciseDao().delete(ex);
                                exercises.remove(pos);
                                adapter.notifyItemRemoved(pos);
                                updatePositions();
                            })
                            .setNegativeButton("Nein", (d, which) -> adapter.notifyItemChanged(pos))
                            .setOnCancelListener(d -> adapter.notifyItemChanged(pos))
                            .create();

                    dialog.show();

                } else if (direction == ItemTouchHelper.LEFT) {
                    // → Links: Umbennen
                    AlertDialog.Builder builder = new AlertDialog.Builder(ExercisesActivity.this);
                    builder.setTitle("Übung umbenennen");

                    EditText input = new EditText(ExercisesActivity.this);
                    input.setText(ex.name); // aktuellen Namen setzen
                    builder.setView(input);

                    builder.setPositiveButton("Speichern", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            ex.name = newName;
                            db.exerciseDao().update(ex);
                            adapter.notifyItemChanged(pos);
                        } else {
                            adapter.notifyItemChanged(pos); // Name leer → zurücksetzen
                        }
                    });

                    builder.setNegativeButton("Abbrechen", (dialog, which) -> adapter.notifyItemChanged(pos));
                    builder.setOnCancelListener(d -> adapter.notifyItemChanged(pos));

                    builder.show();
                }
            }



            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                for (int i = 0; i < exercises.size(); i++) {
                    exercises.get(i).position = i;           // aktuelle Position in RecyclerView
                    exercises.get(i).originalPosition = i;   // neue natürliche Reihenfolge
                    db.exerciseDao().update(exercises.get(i));
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                for (Exercise e : exercises) {
                    if (e.completed) {
                        new AlertDialog.Builder(ExercisesActivity.this)
                                .setTitle("Sortieren blockiert")
                                .setMessage("Einige Übungen sind erledigt. Alle auf uncompleted setzen?")
                                .setPositiveButton("Ja", (dialog, which) -> resetAllCheckboxes())
                                .setNegativeButton("Nein", null)
                                .show();
                        return false;
                    }
                }
                return true;
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void setupActivityResultLauncher() {
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        int newDayId = result.getData().getIntExtra("selected_day_id", -1);
                        if (newDayId != -1) loadTrainingDay(newDayId);
                    }
                }
        );
    }

    private void setupButtons() {
        ImageButton changeDayButton = findViewById(R.id.change_day_button);
        changeDayButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkoutsActivity.class);
            intent.putExtra("from_button", true);
            launcher.launch(intent);
        });

        Button addButton = findViewById(R.id.add_exercise);
        addButton.setOnClickListener(v -> showAddDialog());

        ImageButton resetButton = findViewById(R.id.reset_checkboxes_button);
        resetButton.setOnClickListener(v -> {
            new AlertDialog.Builder(ExercisesActivity.this)
                    .setTitle("Checkboxen zurücksetzen")
                    .setMessage("Möchtest du wirklich alle Übungen und Sets auf uncompleted setzen?")
                    .setPositiveButton("Ja", (dialog, which) -> resetAllCheckboxes())
                    .setNegativeButton("Nein", null)
                    .show();
        });
    }
    private void resetAllCheckboxes() {
        for (Exercise exercise : exercises) {
            exercise.completed = false;
            db.exerciseDao().update(exercise);

            List<ExerciseSet> sets = db.exerciseSetDao().getForExercise(exercise.id);
            for (ExerciseSet set : sets) {
                set.completed = false;
                db.exerciseSetDao().update(set);
            }
        }

        // alle Items erzwingen neu zu binden → Checkboxen und Strike-Through zurücksetzen
        for (int i = 0; i < exercises.size(); i++) {
            adapter.notifyItemChanged(i);
        }

        // Danach Liste sortieren (optional animiert)
        onExerciseCompletedChanged();
    }
    private void loadTrainingDay(int dayId) {
        trainingDayId = dayId;
        refreshExercises();
        saveLastTrainingDay();
    }

    private void refreshExercises() {
        TrainingDay currentDay = db.trainingDayDao().getById(trainingDayId);
        if (currentDay != null) currentDayText.setText(currentDay.name);

        exercises.clear();
        exercises.addAll(db.exerciseDao().getForTrainingDay(trainingDayId));
        adapter.notifyDataSetChanged();
    }

    private void saveLastTrainingDay() {
        getSharedPreferences("GymPrefs", MODE_PRIVATE)
                .edit()
                .putInt("last_day_id", trainingDayId)
                .apply();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Neue Übung");

        EditText input = new EditText(this);
        input.setHint("z.B. Bankdrücken");
        builder.setView(input);

        builder.setPositiveButton("Hinzufügen", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Exercise exercise = new Exercise();
                exercise.name = name;
                exercise.trainingDayId = trainingDayId;
                exercise.completed = false;
                exercise.position = exercises.size();
                exercise.originalPosition = exercises.size();

                long id = db.exerciseDao().insert(exercise);
                exercise.id = (int) id;

                exercises.add(exercise);
                adapter.notifyItemInserted(exercises.size() - 1);
            }
        });

        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }

    private void sortExercises() {
        Collections.sort(exercises, (e1, e2) -> {
            if (e1.completed != e2.completed) {
                return Boolean.compare(e1.completed, e2.completed);
            } else {
                return Integer.compare(e1.originalPosition, e2.originalPosition);
            }
        });
    }
    private void onExerciseCompletedChanged() {
        List<Exercise> oldList = new ArrayList<>(exercises);

        sortExercises(); // sortiert die Liste nach completed & originalPosition
        updatePositions(); // DB aktualisieren

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return exercises.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).id == exercises.get(newItemPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Exercise oldEx = oldList.get(oldItemPosition);
                Exercise newEx = exercises.get(newItemPosition);
                return oldEx.completed == newEx.completed &&
                        oldEx.name.equals(newEx.name) &&
                        oldEx.position == newEx.position;
            }
        });

        diffResult.dispatchUpdatesTo(adapter); // Animierte Umordnung
    }


    private void updatePositions() {
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).position = i;
            db.exerciseDao().update(exercises.get(i));
        }
    }

}
