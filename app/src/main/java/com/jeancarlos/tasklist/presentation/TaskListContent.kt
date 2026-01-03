package com.jeancarlos.tasklist.presentation

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jeancarlos.tasklist.data.TaskItem
import com.jeancarlos.tasklist.presentation.components.CompletedHeader
import com.jeancarlos.tasklist.presentation.components.DeleteAllDialog
import com.jeancarlos.tasklist.presentation.components.EmptyStateView
import com.jeancarlos.tasklist.presentation.components.ProgressFooter
import com.jeancarlos.tasklist.presentation.components.TaskCard
import com.jeancarlos.tasklist.presentation.components.TaskInputSection
import com.jeancarlos.tasklist.presentation.components.WeekCalendarStrip
import com.jeancarlos.tasklist.ui.theme.AppDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Função para dar um "feedback" quando o usuário completa algo.
 * Ele faz o celular vibrar de leve e faz um barulhinho de clique.
 */
@Composable
private fun rememberSuccessFeedback(): () -> Unit {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    return remember {
        {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
            } catch (_: Exception) { }
        }
    }
}

/**
 * Este é o componente que organiza a lista e as interações principais da tela.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskListContent(
    modifier: Modifier = Modifier,
    filteredTasks: List<TaskItem>, // Lista de tarefas filtradas
    allTasksCount: Int,           // Quantidade total
    newTaskText: String,          // O que o usuário está escrevendo
    completedTasks: Int,          // Quantas já foram
    totalTasks: Int,              // Total do dia
    progressPercent: Float,       // Porcentagem da barra
    showCompletedHistory: Boolean,
    isTtsEnabled: Boolean,
    selectedDate: LocalDate,
    finishedTask: TaskItem?,      // Tarefa que acabou de "vencer" o tempo
    onClearFinishedTask: () -> Unit,
    actions: TaskListActions      // Todas as ações que o ViewModel pode fazer
) {
    val context = LocalContext.current
    // TextToSpeech serve para o Android "falar" com a gente
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    
    // Configura a voz assim que a tela abre
    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.forLanguageTag("pt-BR")
            }
        }
    }

    // Se uma tarefa terminou o tempo, a gente faz o celular vibrar e o app falar
    if (finishedTask != null) {
        val vibrator = remember(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }

        DisposableEffect(finishedTask) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)) 

            if (isTtsEnabled) {
                tts.value?.speak(
                    "Atenção! O tempo da tarefa ${finishedTask.name} acabou. O que deseja fazer?",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }

            onDispose {
                vibrator.cancel() // Para de vibrar quando fechar o aviso
                tts.value?.stop() 
            }
        }

        // Mostra o aviso na tela perguntando se quer ir para a próxima tarefa
        TimeFinishedDialog(
            task = finishedTask,
            hasNextTask = filteredTasks.any { !it.isCompleted && it.id != finishedTask.id },
            onConfirmAndStartNext = {
                actions.onToggleComplete(finishedTask)
                actions.onStartNextTask(finishedTask)
                onClearFinishedTask()
            },
            onConfirmAndFinish = {
                actions.onToggleComplete(finishedTask)
                onClearFinishedTask()
            },
            onDismiss = {
                actions.onToggleComplete(finishedTask)
                onClearFinishedTask()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // A faixinha do calendário no topo
        WeekCalendarStrip(
            selectedDate = selectedDate,
            onDateSelected = actions.onDateSelected
        )

        Column(modifier = Modifier.padding(horizontal = AppDimens.spacingLarge)) {
            // Só mostra o campo de digitar se for o dia de HOJE
            if (selectedDate == LocalDate.now()) {
                TaskInputSection(
                    visible = true,
                    text = newTaskText,
                    onTextChange = actions.onNewTaskChange,
                    onAdd = actions.onAddTask
                )
            }

            val pendingTasks = filteredTasks.filter { !it.isCompleted }
            val completedTasksList = filteredTasks.filter { it.isCompleted }

            if (filteredTasks.isEmpty()) {
                // Tela vazia se não tiver tarefas
                EmptyStateView(
                    modifier = Modifier.weight(1f),
                    allTasksCount = allTasksCount,
                    completedTasks = completedTasks,
                    totalTasks = totalTasks
                )
            } else {
                // A lista que "escorrega" (LazyColumn)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingBetweenElements)
                ) {
                    // Tarefas que ainda faltam fazer
                    items(pendingTasks, key = { it.id }) { task ->
                        TaskItemRow(task = task, actions = actions, modifier = Modifier.animateItem())
                    }

                    // Se tiver tarefas terminadas, mostra um cabeçalho para esconder/mostrar
                    if (completedTasksList.isNotEmpty()) {
                        item {
                            CompletedHeader(
                                count = completedTasksList.size,
                                expanded = showCompletedHistory,
                                onClick = actions.onToggleHistory
                            )
                        }

                        if (showCompletedHistory) {
                            items(completedTasksList, key = { it.id }) { task ->
                                TaskItemRow(
                                    task = task,
                                    actions = actions,
                                    modifier = Modifier.animateItem().alpha(0.6f) // Fica meio transparente
                                )
                            }
                        }
                    }
                }
            }

            // A barra de progresso no rodapé
            if (totalTasks > 0) {
                ProgressFooter(
                    progressPercent = (progressPercent * 100).toInt(),
                    completedTasks = completedTasks,
                    totalTasks = totalTasks
                )
            }
        }
    }
}

/**
 * Este componente cuida de uma única "linha" de tarefa na lista.
 * Ele permite que a gente arraste para o lado para deletar (SwipeToDismiss).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TaskItemRow(
    task: TaskItem,
    actions: TaskListActions,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val performSuccessFeedback = rememberSuccessFeedback()
    val isVisible = remember { mutableStateOf(true) }

    // Controla o movimento de arrastar para deletar
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                isVisible.value = false // Esconde visualmente
                scope.launch {
                    delay(400L) // Espera a animação acabar
                    actions.onDeleteTask(task) // Deleta do banco
                }
                return@rememberSwipeToDismissBoxState true
            }
            false
        }
    )

    // Efeito de animação quando a tarefa some
    AnimatedVisibility(
        visible = isVisible.value,
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                // Cor de fundo que aparece quando a gente arrasta (vermelho para deletar)
                val colorState = animateColorAsState(
                    targetValue = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }, label = ""
                )
                Box(
                    modifier = Modifier.fillMaxSize().padding(vertical = AppDimens.heightSmall)
                        .clip(RoundedCornerShape(AppDimens.spacingLarge)).background(colorState.value),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            content = {
                // O cartão da tarefa em si (com nome, prioridade e timer)
                TaskCard(
                    task = task,
                    onToggleComplete = {
                        scope.launch {
                            if (!task.isCompleted) performSuccessFeedback() // Feedback se estiver terminando
                            actions.onToggleComplete(task)
                        }
                    },
                    onStartTask = { actions.onStartTask(task) },
                    onPauseTask = { actions.onPauseTask(task) },
                    onSetTime = { timeInMillis -> actions.onSetTime(task, timeInMillis) },
                    onResetProgress = { actions.onResetProgress(task) },
                    onTimeFinished = { actions.onTaskFinished(task) },
                    onSetPriority = { priority -> actions.onSetPriority(task, priority) },
                    onEditTitle = { newTitle -> actions.onEditTaskTitle(task, newTitle) }
                )
            }
        )
    }
}

/**
 * Janelinha de aviso que aparece quando o cronômetro chega em zero.
 */
@Composable
private fun TimeFinishedDialog(
    task: TaskItem,
    hasNextTask: Boolean,
    onConfirmAndStartNext: () -> Unit,
    onConfirmAndFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tempo Esgotado! \uD83C\uDF89") },
        text = {
            if (hasNextTask) {
                Text("O tempo da tarefa '${task.name}' acabou.\nDeseja iniciar a próxima?")
            } else {
                Text("O tempo da tarefa '${task.name}' acabou.\nFinalizando tarefa.")
            }
        },
        confirmButton = {
            if (hasNextTask) {
                Button(onClick = onConfirmAndStartNext) { Text("Sim, próxima") }
            } else {
                Button(onClick = onConfirmAndFinish) { Text("Concluir") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Não, parar") }
        }
    )
}
