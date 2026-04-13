package com.example.gym.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gym.R;
import com.example.gym.adapter.TrainingDayAdapter;
import com.example.gym.data.database.AppDatabase;
import com.example.gym.data.entity.TrainingDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkoutsActivity extends AppCompatActivity {

    private List<TrainingDay> days;
    private TrainingDayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppDatabase db = AppDatabase.getInstance(this);

        // 🔹 1️⃣ Tage laden, null-sicher
        days = db.trainingDayDao().getAll();
        if (days == null) {
            days = new ArrayList<>();
        }

        // 🔹 2️⃣ fromButton prüfen
        boolean fromButton = getIntent().getBooleanExtra("from_button", false);

        // 🔹 3️⃣ Automatischer Sprung zu Exercises, wenn App normal gestartet
        if (!fromButton && !days.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("GymPrefs", MODE_PRIVATE);
            int lastDayId = prefs.getInt("last_day_id", -1);

            if (lastDayId != -1) {
                TrainingDay lastDay = db.trainingDayDao().getById(lastDayId);
                if (lastDay != null) {
                    Intent intent = new Intent(this, ExercisesActivity.class);
                    intent.putExtra("trainingDayId", lastDay.id);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        }

        // 🔹 4️⃣ Layout setzen
        setContentView(R.layout.activity_workouts);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 5️⃣ Adapter mit Callback für Klick
        adapter = new TrainingDayAdapter(days, day -> {
            if (fromButton) {
                Intent result = new Intent();
                result.putExtra("selected_day_id", day.id);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Intent intent = new Intent(WorkoutsActivity.this, ExercisesActivity.class);
                intent.putExtra("trainingDayId", day.id);
                startActivity(intent);
                finish(); // 🔹 aktuelle Activity schließen, Backstack sauber halten
            }
        });

        recyclerView.setAdapter(adapter);

        // 🔹 6️⃣ Drag & Drop + Swipe
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                ) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int from = viewHolder.getAdapterPosition();
                        int to = target.getAdapterPosition();

                        Collections.swap(days, from, to);
                        adapter.notifyItemMoved(from, to);
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) return;

                        TrainingDay day = days.get(position);

                        if (direction == ItemTouchHelper.RIGHT) {
                            // → Rechts: Löschen
                            AlertDialog dialog = new AlertDialog.Builder(WorkoutsActivity.this)
                                    .setTitle("Löschen bestätigen")
                                    .setMessage("Möchtest du den Trainingstag wirklich löschen?")
                                    .setPositiveButton("Ja", (d, which) -> {
                                        db.trainingDayDao().delete(day);
                                        days.remove(position);
                                        adapter.notifyItemRemoved(position);
                                    })
                                    .setNegativeButton("Nein", (d, which) -> adapter.notifyItemChanged(position))
                                    .setOnCancelListener(d -> adapter.notifyItemChanged(position))
                                    .create();

                            dialog.show();

                        } else if (direction == ItemTouchHelper.LEFT) {
                            // → Links: Umbennen
                            AlertDialog.Builder builder = new AlertDialog.Builder(WorkoutsActivity.this);
                            builder.setTitle("Trainingstag umbenennen");

                            EditText input = new EditText(WorkoutsActivity.this);
                            input.setText(day.name); // aktuellen Namen setzen
                            builder.setView(input);

                            builder.setPositiveButton("Speichern", (dialog, which) -> {
                                String newName = input.getText().toString().trim();
                                if (!newName.isEmpty()) {
                                    day.name = newName;
                                    db.trainingDayDao().update(day);
                                    adapter.notifyItemChanged(position);
                                } else {
                                    adapter.notifyItemChanged(position); // leer → zurücksetzen
                                }
                            });

                            builder.setNegativeButton("Abbrechen", (dialog, which) -> adapter.notifyItemChanged(position));
                            builder.setOnCancelListener(d -> adapter.notifyItemChanged(position));

                            builder.show();
                        }
                    }



                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);
                        for (int i = 0; i < days.size(); i++) {
                            days.get(i).position = i;
                            db.trainingDayDao().update(days.get(i));
                        }
                    }

                    @Override
                    public boolean isLongPressDragEnabled() {
                        return true;
                    }
                };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);

        // 🔹 7️⃣ Button zum Hinzufügen eines TrainingDays
        Button addButton = findViewById(R.id.add_day);
        addButton.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Neuer Trainingstag");

        EditText input = new EditText(this);
        input.setHint("Name eingeben");
        builder.setView(input);

        builder.setPositiveButton("Hinzufügen", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                AppDatabase db = AppDatabase.getInstance(this);
                TrainingDay newDay = new TrainingDay();
                newDay.name = name;
                newDay.position = days.size();

                long id = db.trainingDayDao().insert(newDay);
                newDay.id = (int) id;

                days.add(newDay);
                adapter.notifyItemInserted(days.size() - 1);
            }
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onBackPressed() {
        boolean fromButton = getIntent().getBooleanExtra("from_button", false);

        if (fromButton) {
            // Zurück an aufrufende Activity ohne Auswahl
            setResult(RESULT_CANCELED);
            super.onBackPressed();
            return;
        }

        // Prüfen: existiert noch mindestens ein TrainingDay?
        if (days.isEmpty()) {
            // Kein Trainingstag mehr → App sauber schließen
            finishAffinity(); // beendet alle Activities
            return;
        }

        // Prüfen: existiert der last_day_id noch?
        SharedPreferences prefs = getSharedPreferences("GymPrefs", MODE_PRIVATE);
        int lastDayId = prefs.getInt("last_day_id", -1);

        boolean valid = false;
        for (TrainingDay day : days) {
            if (day.id == lastDayId) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            prefs.edit().remove("last_day_id").apply();
            // Kein gültiger TrainingsDay mehr → App sauber schließen
            finishAffinity();
            return;
        }

        super.onBackPressed();
    }





}
