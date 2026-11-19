package com.example.tasks

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.tasks.data.SettingsRepository
import com.example.tasks.data.TasksDatabase

private val Context.dataStore by preferencesDataStore(name = "task_settings")

class TasksApplication : Application() {
    val database: TasksDatabase by lazy {
        Room.databaseBuilder(this, TasksDatabase::class.java, "tasks.db").build()
    }
    val settingsRepository by lazy { SettingsRepository(dataStore) }
}
