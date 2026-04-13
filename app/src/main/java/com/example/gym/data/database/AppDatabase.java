package com.example.gym.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.gym.data.dao.ExerciseDao;
import com.example.gym.data.dao.ExerciseSetDao;
import com.example.gym.data.dao.TrainingDayDao;
import com.example.gym.data.entity.Exercise;
import com.example.gym.data.entity.ExerciseSet;
import com.example.gym.data.entity.TrainingDay;

@Database(entities = {TrainingDay.class, Exercise.class, ExerciseSet.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {


    private static AppDatabase INSTANCE;


    public abstract TrainingDayDao trainingDayDao();
    public abstract ExerciseDao exerciseDao();
    public abstract ExerciseSetDao exerciseSetDao();


    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "gym_db"
            ).allowMainThreadQueries().build();
        }
        return INSTANCE;
    }
}