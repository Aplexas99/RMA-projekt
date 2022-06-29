package com.example.myapplication1.data

import androidx.room.*
import com.example.myapplication1.models.Run


@Dao
interface RunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(run:Run)

    @Delete
    fun delete(run:Run)

    @Query("SELECT * FROM runs WHERE id=:id")
    fun getRunById(id:Long):Run?

    @Query("SELECT * FROM runs")
    fun getAllRuns(): List<Run>

}