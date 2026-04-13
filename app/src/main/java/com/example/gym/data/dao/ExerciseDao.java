package com.example.gym.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.gym.data.entity.Exercise;

import java.util.List;

@Dao
public interface ExerciseDao {

    @Query("SELECT * FROM exercise WHERE trainingDayId = :dayId ORDER BY position ASC")
    List<Exercise> getForTrainingDay(int dayId);

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    Exercise getById(int id);  // <-- neue Methode

    @Insert
    long insert(Exercise exercise); // <-- ID zurückgeben

    @Update
    void update(Exercise exercise);

    @Delete
    void delete(Exercise exercise);
}
