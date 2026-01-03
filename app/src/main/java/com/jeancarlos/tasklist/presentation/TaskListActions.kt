package com.jeancarlos.tasklist.presentation

import com.jeancarlos.tasklist.data.Priority
import com.jeancarlos.tasklist.data.TaskItem
import java.time.LocalDate

data class TaskListActions(
    val onNewTaskChange: (String) -> Unit,
    val onAddTask: () -> Unit,
    val onToggleComplete: (TaskItem) -> Unit,
    val onStartTask: (TaskItem) -> Unit,
    val onPauseTask: (TaskItem) -> Unit,
    val onDeleteTask: (TaskItem) -> Unit,
    val onSetTime: (TaskItem, Long) -> Unit,
    val onResetProgress: (TaskItem) -> Unit,
    val onTaskFinished: (TaskItem) -> Unit,
    val onDeleteAll: () -> Unit,
    val onRestoreAll: () -> Unit,
    val onToggleHistory: () -> Unit,
    val onStartNextTask: (TaskItem) -> Unit,
    val onSetPriority: (TaskItem, Priority) -> Unit,
    val onEditTaskTitle: (TaskItem, String) -> Unit,
    val onToggleTts: () -> Unit,
    val onDateSelected: (LocalDate) -> Unit // NOVO
)
