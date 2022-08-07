package com.devtools.dogsapp.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;


import java.util.List;

@Dao
public interface DogDao {
    @Insert // returns a list of integers = priary keys of dogbreeds
    List<Long> insertAll(DogBreed... dogs); // ... provide AS MANY DOG VARIABLES AS WE LIKE.

    @Query("select * from dogbreed")
    List<DogBreed> getAllDogs();

    @Query("select * from dogbreed where uuid = :dogId")
    DogBreed getDog(int dogId);

    @Query("DELETE FROM dogbreed")
    void deleteAllDogs();
}
