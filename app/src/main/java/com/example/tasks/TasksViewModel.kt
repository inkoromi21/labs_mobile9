package com.example.tasks

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasks.data.Task
import com.example.tasks.data.TaskSettings
import com.example.tasks.data.TaskSort
import com.example.tasks.data.sortTasks
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TasksUiState(
    val settings: TaskSettings? = null,
    val tasks: List<Task> = emptyList(),
    val error: String? = null
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TasksApplication
    private val dao = app.database.taskDao()
    private val settingsRepository = app.settingsRepository
    private var loadJob: Job? = null

    var uiState by mutableStateOf(TasksUiState())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init { load() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load() {
        loadJob?.cancel()
        uiState = TasksUiState()
        loadJob = viewModelScope.launch {
            try {
                settingsRepository.settings.flatMapLatest { settings ->
                    dao.tasksForDate(settings.date.toEpochDay()).map { tasks ->
                        TasksUiState(settings, sortTasks(tasks, settings.sort))
                    }
                }.collect { uiState = it }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                uiState = TasksUiState(error = "Не удалось прочитать данные. Повторите попытку.")
            }
        }
    }

    fun chooseDate(date: LocalDate) {
        viewModelScope.launch { write { settingsRepository.setDate(date) } }
    }

    fun chooseSort(sort: TaskSort) {
        viewModelScope.launch { write { settingsRepository.setSort(sort) } }
    }

    fun taskById(id: Long) = dao.taskById(id)

    suspend fun save(task: Task): Boolean {
        if (task.title.isBlank() || task.minuteOfDay !in 0..1439) return false
        return write {
            val savedTask = task.copy(title = task.title.trim(), description = task.description.trim())
            if (savedTask.id == 0L) dao.insert(savedTask) else dao.update(savedTask)
        }
    }

    suspend fun delete(task: Task): Boolean = write { dao.delete(task) }

    private suspend fun write(action: suspend () -> Unit): Boolean {
        if (busy) return false
        busy = true
        return try {
            action()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            message = "Не удалось сохранить изменения. Повторите попытку."
            false
        } finally {
            busy = false
        }
    }

    fun clearMessage() { message = null }
}
