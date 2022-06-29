package com.example.myapplication1.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "runs")

data class Run (
    @PrimaryKey(autoGenerate = true)
    var id: Long=0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "duration")
    val duration: String,
    @ColumnInfo(name = "pace")
    val pace: String,
    @ColumnInfo(name = "distance")
    val distance: String,
    @ColumnInfo(name = "dateAdded")
    val dateAdded: String
    )
{
}