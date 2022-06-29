package com.example.myapplication1.data.room


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication1.data.RunDao
import com.example.myapplication1.models.Run
import java.security.AccessControlContext


@Database(
    entities = [Run::class],
    version = 1,
    exportSchema = false
)

abstract class RunDatabase: RoomDatabase() {

    abstract fun getRunDao(): RunDao

    companion object{
        private const val databaseName = "runsDb"

        @Volatile
        private var INSTANCE: RunDatabase? = null

        fun getDatabase(context: Context): RunDatabase{
            if(INSTANCE == null){
                synchronized(this){
                    INSTANCE = buildDatabase(context)
                }
            }
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): RunDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                RunDatabase::class.java,
                databaseName
            )
                    .allowMainThreadQueries()
                .build()
        }
    }

}