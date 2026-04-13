package com.example.gym.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class TrainingDay {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int position;
}