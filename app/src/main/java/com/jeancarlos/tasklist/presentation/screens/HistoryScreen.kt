package com.jeancarlos.tasklist.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jeancarlos.tasklist.data.TaskItem
import com.jeancarlos.tasklist.presentation.TaskListViewModel
import com.jeancarlos.tasklist.presentation.components.DeleteAllDialog
import com.jeancarlos.tasklist.presentation.components.TaskCard
import com.jeancarlos.tasklist.ui.theme.AppDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TaskListViewModel,
    onBack: () -> Unit
) {
    val completedTasks by viewModel.fullHistoryTasks.collectAsState()
    
    val showMenu = remember { mutableStateOf(false) }
    val showDeleteConfirmation = remember { mutableStateOf(false) }

    if (showDeleteConfirmation.value) {
        DeleteAllDialog(
            onConfirm = {
                viewModel.deleteAll()
                showDeleteConfirmation.value = false
            },
            onDismiss = { showDeleteConfirmation.value = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico Completo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(expanded = showMenu.value, onDismissRequest = { showMenu.value = false }) {
                        DropdownMenuItem(
                            text = { Text("Restaurar Todas") },
                            leadingIcon = { Icon(Icons.Default.Restore, null) },
                            onClick = { 
                                showMenu.value = false
                                viewModel.restoreAll() 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zerar Todos os Tempos") },
                            leadingIcon = { Icon(Icons.Default.Timer, null) },
                            onClick = { 
                                showMenu.value = false
                                viewModel.resetAllTime() 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir Todas", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { 
                                showMenu.value = false
                                showDeleteConfirmation.value = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = AppDimens.spacingLarge)
        ) {
            if (completedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma tarefa concluída ainda.", color = Color.Gray)
                }
            } else {
                val groupedTasks = remember(completedTasks) {
                    val locale = Locale.forLanguageTag("pt-BR")
                    val formatter = SimpleDateFormat("EEEE, dd 'de' MMMM", locale)
                    
                    completedTasks.groupBy { task ->
                        val date = task.completedAt?.let { 
                            Date(it)
                        } ?: Date()
                        
                        formatter.format(date)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spacingSmall)
                ) {
                    groupedTasks.forEach { (date, tasks) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = AppDimens.spacingLarge, bottom = AppDimens.spacingSmall),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(tasks, key = { it.id }) { task ->
                            HistoryTaskItemRow(
                                task = task,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTaskItemRow(
    task: TaskItem,
    viewModel: TaskListViewModel
) {
    val scope = rememberCoroutineScope()
    val isVisible = remember { mutableStateOf(true) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                isVisible.value = false
                scope.launch {
                    delay(400L)
                    viewModel.deleteTask(task)
                }
                return@rememberSwipeToDismissBoxState true
            }
            false
        }
    )

    AnimatedVisibility(
        visible = isVisible.value,
        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) +
                fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    },
                    label = "background color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = AppDimens.heightSmall)
                        .clip(RoundedCornerShape(AppDimens.spacingLarge))
                        .background(color),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir permanentemente",
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            content = {
                TaskCard(
                    task = task,
                    onToggleComplete = { _ -> viewModel.toggleComplete(task) },
                    onStartTask = { },
                    onPauseTask = { _ -> },
                    onSetTime = { _ -> },
                    onResetProgress = { },
                    onTimeFinished = { },
                    onSetPriority = { _ -> },
                    onEditTitle = { _ -> }
                )
            }
        )
    }
}