package com.example.myapplication1.data

import com.example.myapplication1.models.Run

class RunRepositoryImpl(val runDao: RunDao): RunRepository {

    override fun save(run: Run) = runDao.save(run)
    override fun delete(run: Run) = runDao.delete(run)
    override fun getRunById(id: Long): Run? = runDao.getRunById(id)
    override fun getAllRuns(): List<Run> = runDao.getAllRuns()

}