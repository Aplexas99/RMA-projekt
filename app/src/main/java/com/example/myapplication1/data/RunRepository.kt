package com.example.myapplication1.data

import com.example.myapplication1.models.Run

interface RunRepository {

    fun save(run: Run)
    fun delete(run: Run)
    fun getRunById(id:Long): Run?
    fun getAllRuns(): List<Run>

}