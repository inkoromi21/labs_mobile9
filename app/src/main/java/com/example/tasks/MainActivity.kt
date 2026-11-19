package com.example.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasks.data.Task
import com.example.tasks.data.TaskSettings
import com.example.tasks.data.TaskSort
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun taskTime(minute: Int): String = LocalTime.of(minute / 60, minute % 60).format(timeFormat)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF365E9D),
                    background = Color(0xFFF7F8FA),
                    surface = Color(0xFFF7F8FA)
                )
            ) { TasksApp() }
        }
    }
}

private data class TaskDetails(
    val id: Long = 0,
    val loaded: Boolean = false,
    val task: Task? = null,
    val error: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksApp(model: TasksViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf("list") }
    var taskId by rememberSaveable { mutableStateOf(0L) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val state = model.uiState
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val taskFlow = remember(taskId, screen) {
        val selectedId = taskId
        model.taskById(selectedId).map { TaskDetails(selectedId, loaded = true, task = it) }
            .catch { emit(TaskDetails(selectedId, loaded = true, error = true)) }
    }
    val details by taskFlow.collectAsStateWithLifecycle(initialValue = TaskDetails())
    val currentTask = details.task?.takeIf { it.id == taskId }

    fun goBack() {
        screen = if (screen == "edit" && taskId != 0L) "detail" else "list"
        confirmDelete = false
    }

    BackHandler(enabled = screen != "list" && !model.busy) { goBack() }
    BackHandler(enabled = screen != "list" && model.busy) { }

    LaunchedEffect(model.message) {
        model.message?.let {
            snackbar.showSnackbar(it)
            model.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (screen) {
                        "detail" -> "Детали задачи"
                        "edit" -> if (taskId == 0L) "Новая задача" else "Редактирование"
                        else -> "Tasks"
                    })
                },
                navigationIcon = {
                    if (screen != "list") {
                        TextButton(onClick = { goBack() }, enabled = !model.busy) { Text("Назад") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (screen == "list" && state.settings != null && !model.busy) {
                FloatingActionButton(
                    onClick = { taskId = 0; screen = "edit" },
                    modifier = Modifier.semantics { contentDescription = "Добавить задачу" }
                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
            }
        }
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when {
            state.error != null -> Box(body, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, modifier = Modifier.padding(24.dp))
                    Button(onClick = model::load) { Text("Повторить") }
                }
            }
            state.settings == null -> Box(body, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            screen == "list" -> TaskList(
                state.settings, state.tasks, model.busy, model::chooseDate, model::chooseSort,
                onTask = { taskId = it.id; screen = "detail" }, modifier = body
            )
            taskId != 0L && (!details.loaded || details.id != taskId) -> Box(body, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            taskId != 0L && details.error -> Column(body.padding(24.dp)) {
                Text("Не удалось загрузить задачу. Откройте её повторно из списка.")
                TextButton(onClick = { screen = "list" }) { Text("К списку") }
            }
            taskId != 0L && currentTask == null -> Column(body.padding(24.dp)) {
                Text("Задача удалена.")
                TextButton(onClick = { screen = "list" }) { Text("К списку") }
            }
            screen == "edit" -> TaskEditor(
                currentTask.takeIf { taskId != 0L }, state.settings.date, model.busy,
                onSave = { task ->
                    scope.launch {
                        if (model.save(task)) {
                            if (task.id == 0L) model.chooseDate(LocalDate.ofEpochDay(task.date))
                            screen = if (task.id == 0L) "list" else "detail"
                        }
                    }
                },
                modifier = body
            )
            currentTask != null -> TaskDetail(
                currentTask, model.busy,
                onEdit = { screen = "edit" }, onDelete = { confirmDelete = true }, modifier = body
            )
        }
    }

    if (confirmDelete && currentTask != null) {
        AlertDialog(
            onDismissRequest = { if (!model.busy) confirmDelete = false },
            title = { Text("Удалить задачу?") },
            text = { Text("«${currentTask.title}» будет удалена.") },
            confirmButton = {
                TextButton(
                    enabled = !model.busy,
                    onClick = {
                        scope.launch {
                            if (model.delete(currentTask)) {
                                confirmDelete = false
                                screen = "list"
                            }
                        }
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = !model.busy) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TaskList(
    settings: TaskSettings,
    tasks: List<Task>,
    busy: Boolean,
    onDate: (LocalDate) -> Unit,
    onSort: (TaskSort) -> Unit,
    onTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortMenu by remember { mutableStateOf(false) }
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onDate(settings.date.minusDays(1)) },
                enabled = !busy && settings.date > LocalDate.now(),
                modifier = Modifier.semantics { contentDescription = "Предыдущий день" }
            ) { Text("←") }
            OutlinedButton(
                onClick = { showDatePicker(context, settings.date, onDate) },
                enabled = !busy, modifier = Modifier.weight(1f)
            ) { Text(settings.date.format(dateFormat)) }
            TextButton(
                onClick = { onDate(settings.date.plusDays(1)) }, enabled = !busy,
                modifier = Modifier.semantics { contentDescription = "Следующий день" }
            ) { Text("→") }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onDate(LocalDate.now()) }, enabled = !busy) { Text("Сегодня") }
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { sortMenu = true }, enabled = !busy) { Text(settings.sort.label) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    TaskSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.label) },
                            onClick = { sortMenu = false; onSort(sort) }
                        )
                    }
                }
            }
        }
        Text("Задач: ${tasks.size}", style = MaterialTheme.typography.labelLarge)
        if (tasks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("На этот день задач нет", style = MaterialTheme.typography.titleMedium)
                    Text("Нажмите +, чтобы добавить задачу.", modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth().clickable { onTask(task) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(taskTime(task.minuteOfDay), color = MaterialTheme.colorScheme.primary)
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            if (task.description.isNotEmpty()) {
                                Text(task.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDetail(
    task: Task,
    busy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(task.title, style = MaterialTheme.typography.headlineSmall)
        Text("${LocalDate.ofEpochDay(task.date).format(dateFormat)} · ${taskTime(task.minuteOfDay)}")
        Text(task.description.ifBlank { "Описание не добавлено" })
        Button(onClick = onEdit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Редактировать")
        }
        OutlinedButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Удалить", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun TaskEditor(
    task: Task?,
    selectedDate: LocalDate,
    busy: Boolean,
    onSave: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val id = task?.id ?: 0L
    var title by rememberSaveable(id) { mutableStateOf(task?.title ?: "") }
    var description by rememberSaveable(id) { mutableStateOf(task?.description ?: "") }
    var date by rememberSaveable(id) { mutableStateOf(task?.date ?: selectedDate.toEpochDay()) }
    var minute by rememberSaveable(id) {
        mutableStateOf(task?.minuteOfDay ?: LocalTime.now().let { it.hour * 60 + it.minute })
    }
    val context = LocalContext.current
    Column(
        modifier.imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Название *") }, singleLine = true, enabled = !busy,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = {
                showDatePicker(context, LocalDate.ofEpochDay(date)) { date = it.toEpochDay() }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text("Дата: ${LocalDate.ofEpochDay(date).format(dateFormat)}") }
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context, { _, hour, minutes -> minute = hour * 60 + minutes },
                    minute / 60, minute % 60, true
                ).show()
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()
        ) { Text("Время: ${taskTime(minute)}") }
        OutlinedTextField(
            value = description, onValueChange = { description = it },
            label = { Text("Описание") }, minLines = 4, enabled = !busy,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Text("* Обязательное поле", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { onSave(Task(id, title, description, date, minute)) },
            enabled = title.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Сохранение…" else "Сохранить") }
        Spacer(Modifier.height(16.dp))
    }
}

private fun showDatePicker(
    context: android.content.Context,
    date: LocalDate,
    onDate: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
        date.year, date.monthValue - 1, date.dayOfMonth
    ).show()
}
