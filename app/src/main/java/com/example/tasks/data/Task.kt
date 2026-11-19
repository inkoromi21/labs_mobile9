package com.example.tasks.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.Collator
import java.util.Locale

@Entity(tableName = "tasks", indices = [Index("date")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val date: Long,
    val minuteOfDay: Int
)

enum class TaskSort(val label: String) {
    DATE_ASC("Дата и время ↑"),
    DATE_DESC("Дата и время ↓"),
    TITLE("По алфавиту")
}

fun sortTasks(tasks: List<Task>, sort: TaskSort): List<Task> = when (sort) {
    TaskSort.DATE_ASC -> tasks.sortedWith(
        compareBy<Task> { it.date }.thenBy { it.minuteOfDay }.thenBy { it.id }
    )
    TaskSort.DATE_DESC -> tasks.sortedWith(
        compareByDescending<Task> { it.date }.thenByDescending { it.minuteOfDay }.thenBy { it.id }
    )
    TaskSort.TITLE -> tasks.sortedWith(
        compareBy<Task, String>(Collator.getInstance(Locale.forLanguageTag("ru")).apply {
            strength = Collator.PRIMARY
        }) { it.title }.thenBy { it.date }
            .thenBy { it.minuteOfDay }.thenBy { it.id }
    )
}
