package com.example.tasks.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.map

data class TaskSettings(
    val date: LocalDate = LocalDate.now(),
    val sort: TaskSort = TaskSort.DATE_ASC
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val dateKey = longPreferencesKey("selected_date")
    private val sortKey = stringPreferencesKey("sort")

    val settings = dataStore.data.map { preferences ->
        TaskSettings(
            date = preferences[dateKey]?.let(LocalDate::ofEpochDay) ?: LocalDate.now(),
            sort = TaskSort.entries.find { it.name == preferences[sortKey] } ?: TaskSort.DATE_ASC
        )
    }

    suspend fun setDate(date: LocalDate) {
        dataStore.edit { it[dateKey] = date.toEpochDay() }
    }

    suspend fun setSort(sort: TaskSort) {
        dataStore.edit { it[sortKey] = sort.name }
    }
}
