package com.example.gym.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "exercise",
        foreignKeys = @ForeignKey(
                entity = TrainingDay.class,
                parentColumns = "id",
                childColumns = "trainingDayId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("trainingDayId")}
)
public class Exercise {

    @PrimaryKey(autoGenerate = true)  // <-- Primary Key hinzufügen
    public int id;

    public int trainingDayId; // Foreign Key
    public String name;
    public boolean completed;
    public int position;
    public int originalPosition;
}
