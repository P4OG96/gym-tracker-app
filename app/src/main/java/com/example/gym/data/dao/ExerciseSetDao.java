package com.example.gym.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.gym.data.entity.ExerciseSet;

import java.util.List;

@Dao
public interface ExerciseSetDao {

    @Query("SELECT * FROM exercise_set WHERE exerciseId = :exerciseId")
    List<ExerciseSet> getForExercise(int exerciseId);

    @Insert
    long insert(ExerciseSet set); // <-- ID zurückgeben

    @Delete
    void delete(ExerciseSet set);

    @Update
    void update(ExerciseSet set);

    @Query("UPDATE exercise_set SET completed = 0")
    void resetAll();
}
