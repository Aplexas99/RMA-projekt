package com.example.myapplication1.data.memory_db

import com.example.myapplication1.data.RunDao
import com.example.myapplication1.models.Run

class InMemoryDb: RunDao {

    private val runs = mutableListOf<Run>()

    init{

    }

    override fun save(run: Run) {
        runs.add(run)
    }

    override fun delete(run: Run) {
        runs.remove(run)
    }

    override fun getRunById(id: Long): Run? {
        return runs.firstOrNull{ it.id ==id}
    }

    override fun getAllRuns(): List<Run> {
        return runs
    }

}