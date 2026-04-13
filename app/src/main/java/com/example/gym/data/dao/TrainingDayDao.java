package com.example.gym.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.gym.data.entity.TrainingDay;

import java.util.List;

@Dao
public interface TrainingDayDao {

    @Query("SELECT * FROM TrainingDay ORDER BY position ASC")
    List<TrainingDay> getAll();

    @Query("SELECT * FROM TrainingDay WHERE id = :id")
    TrainingDay getById(int id);

    @Insert
    long insert(TrainingDay day); // <-- ID zurückgeben

    @Update
    void update(TrainingDay day);

    @Delete
    void delete(TrainingDay day);
}
