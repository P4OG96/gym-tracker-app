package com.example.gym.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gym.R;
import com.example.gym.data.database.AppDatabase;
import com.example.gym.data.entity.ExerciseSet;
import java.util.List;
import java.util.Locale;

public class SetAdapter extends RecyclerView.Adapter<SetAdapter.ViewHolder> {

    private List<ExerciseSet> sets;
    private AppDatabase db;

    public SetAdapter(List<ExerciseSet> sets, Context context) {
        this.sets = sets;
        this.db = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sets, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseSet set = sets.get(position);

        // Setze die Nummer dynamisch
        holder.setNumber.setText(set.setNumber + ". Set");

        holder.checkBox.setOnCheckedChangeListener(null); // Reset Listener
        holder.checkBox.setChecked(set.completed);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            set.completed = isChecked;
            db.exerciseSetDao().update(set);
        });

        holder.weight.setText(String.valueOf(set.weight));
        holder.weight.setOnClickListener(v -> showNumberPickerDialog(holder.weight, set, true));

        holder.reps.setText(String.valueOf(set.reps));
        holder.reps.setOnClickListener(v -> showNumberPickerDialog(holder.reps, set, false));
    }


    @Override
    public int getItemCount() {
        return sets.size();
    }

    // ---------- ViewHolder ----------
    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        EditText weight, reps;
        TextView setNumber;



        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            weight = itemView.findViewById(R.id.weight);
            reps = itemView.findViewById(R.id.reps);
            setNumber = itemView.findViewById(R.id.set_number);

            // Keine Tastatur, nur Klick
            weight.setInputType(InputType.TYPE_NULL);
            weight.setFocusable(false);
            reps.setInputType(InputType.TYPE_NULL);
            reps.setFocusable(false);
        }
    }


    private void showNumberPickerDialog(EditText editText, ExerciseSet set, boolean isWeight) {
        Context context = editText.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(isWeight ? "Weight" : "Reps");

        if (isWeight) {
            // =========================
            // WEIGHT → 2 PICKER
            // =========================

            // Ganzzahl-Picker
            NumberPicker wholePicker = new NumberPicker(context);
            wholePicker.setMinValue(0);
            wholePicker.setMaxValue(100);
            wholePicker.setWrapSelectorWheel(false);

            // Nachkomma-Picker (0 oder 5)
            NumberPicker decimalPicker = new NumberPicker(context);
            String[] decimals = {"0", "5"};
            decimalPicker.setMinValue(0);
            decimalPicker.setMaxValue(decimals.length - 1);
            decimalPicker.setDisplayedValues(decimals);
            decimalPicker.setWrapSelectorWheel(false);

            // Aktuellen Wert setzen
            float current = set.weight;
            int whole = (int) current;
            int decimalIndex = (current % 1 == 0.5f) ? 1 : 0;

            wholePicker.setValue(whole);
            decimalPicker.setValue(decimalIndex);

            // Punkt zwischen den Pickern
            TextView dot = new TextView(context);
            dot.setText(".");
            dot.setTextSize(24);
            dot.setPadding(8, 0, 8, 0);
            dot.setGravity(Gravity.CENTER);

            // Horizontales Layout
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(10, 10, 10, 10);

            layout.addView(wholePicker);
            layout.addView(dot);
            layout.addView(decimalPicker);

            builder.setView(layout);

            builder.setPositiveButton("✔️", (dialog, which) -> {
                int w = wholePicker.getValue();
                boolean half = decimalPicker.getValue() == 1;

                float value = w + (half ? 0.5f : 0f);

                editText.setText(String.format(Locale.US, "%.1f", value));
                set.weight = value;
                db.exerciseSetDao().update(set);
            });

        } else {
            // =========================
            // REPS → 1 PICKER
            // =========================

            NumberPicker picker = new NumberPicker(context);
            picker.setMinValue(1);
            picker.setMaxValue(100);
            picker.setWrapSelectorWheel(false);

            int currentValue = set.reps > 0 ? set.reps : 1;
            picker.setValue(currentValue);

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(10, 10, 10, 10);
            layout.addView(picker);

            builder.setView(layout);

            builder.setPositiveButton("✔️", (dialog, which) -> {
                int value = picker.getValue();
                editText.setText(String.valueOf(value));
                set.reps = value;
                db.exerciseSetDao().update(set);
            });
        }

        builder.setNegativeButton("✖️", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Dialoggröße kompakt halten
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

}
