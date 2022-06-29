package com.example.myapplication1.di

import com.example.myapplication1.PaceMe
import com.example.myapplication1.ui.MainActivity
import com.example.myapplication1.data.RunRepository
import com.example.myapplication1.data.RunRepositoryImpl
import com.example.myapplication1.data.room.RunDatabase

object RunRepositoryFactory {
    val roomDb = RunDatabase.getDatabase(PaceMe.application)
    val runRepository: RunRepository = RunRepositoryImpl(roomDb.getRunDao())

}