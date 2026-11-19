package com.example.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortTest {
    @Test
    fun sortsByWholeDateAndTimeAndRussianTitle() {
        val tasks = listOf(
            Task(1, "Яблоко", "", 20, 600),
            Task(2, "банан", "", 19, 900),
            Task(3, "Арбуз", "", 20, 500)
        )
        assertEquals(listOf(2L, 3L, 1L), sortTasks(tasks, TaskSort.DATE_ASC).map { it.id })
        assertEquals(listOf(1L, 3L, 2L), sortTasks(tasks, TaskSort.DATE_DESC).map { it.id })
        assertEquals(listOf(3L, 2L, 1L), sortTasks(tasks, TaskSort.TITLE).map { it.id })
        assertEquals(emptyList<Task>(), sortTasks(emptyList(), TaskSort.DATE_ASC))
        val russian = listOf("Жук", "ёж", "Еда").mapIndexed { index, title ->
            Task(index.toLong(), title, "", 20, 500)
        }
        assertEquals(listOf("Еда", "ёж", "Жук"), sortTasks(russian, TaskSort.TITLE).map { it.title })
    }
}
