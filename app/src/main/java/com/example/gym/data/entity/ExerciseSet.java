package com.example.gym.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "exercise_set",
        foreignKeys = @ForeignKey(
                entity = Exercise.class,
                parentColumns = "id",
                childColumns = "exerciseId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("exerciseId")}  // <-- Index hinzufügen
)
public class ExerciseSet {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int exerciseId;  // Foreign Key
    public int setNumber;
    public float weight;
    public int reps;
    public boolean completed;
}
