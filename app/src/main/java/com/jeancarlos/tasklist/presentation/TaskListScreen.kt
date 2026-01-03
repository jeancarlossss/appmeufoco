package com.jeancarlos.tasklist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import com.jeancarlos.tasklist.presentation.components.LottieCelebration
import com.jeancarlos.tasklist.presentation.components.TaskListTopBar

/**
 * Esta é a tela principal do nosso aplicativo.
 * Imagine que ela é a "moldura" que segura tudo o que a gente vê.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreenContent(
    viewModel: TaskListViewModel,       // O cérebro que a gente comentou antes
    onNavigateToStats: () -> Unit,      // Função para ir para a tela de estatísticas
    onNavigateToHistory: () -> Unit      // Função para ir para o histórico completo
) {
    // Aqui a gente "ouve" o ViewModel. Se algo mudar lá, o Compose atualiza a tela aqui.
    val uiState by viewModel.uiState.collectAsState()
    val finishedTask by viewModel.finishedTask.collectAsState()
    
    // Variável simples para controlar se a gente mostra o confete na tela ou não.
    val showConfetti = remember { mutableStateOf(false) }

    // O Scaffold é a estrutura básica do Material Design (barra em cima, conteúdo no meio).
    Scaffold(
        topBar = {
            // A barra de cima com o título e os botões de menu
            TaskListTopBar(
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToStats = onNavigateToStats,
                isTtsEnabled = uiState.isTtsEnabled,
                onToggleTts = viewModel::toggleTts
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // O Box é como um container que permite colocar coisas uma em cima da outra (Z-index).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Criamos um degradê suave no fundo para ficar mais bonito
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            // Este é o conteúdo real: a lista de tarefas, o calendário, etc.
            TaskListContent(
                modifier = Modifier.padding(paddingValues),
                filteredTasks = uiState.tasks, 
                allTasksCount = uiState.totalCount,
                newTaskText = uiState.newTaskText,
                completedTasks = uiState.completedCount,
                totalTasks = uiState.totalCount,
                progressPercent = uiState.progress,
                showCompletedHistory = uiState.showCompletedHistory,
                isTtsEnabled = uiState.isTtsEnabled,
                selectedDate = uiState.selectedDate,

                finishedTask = finishedTask,
                onClearFinishedTask = { viewModel.clearFinishedTask() },

                // Aqui a gente conecta as ações da tela com as funções do ViewModel
                actions = TaskListActions(
                    onNewTaskChange = viewModel::updateNewTaskText,
                    onAddTask = viewModel::addTask,
                    onToggleComplete = { task ->
                        // Se estiver completando a tarefa agora, solta o confete!
                        if (!task.isCompleted) showConfetti.value = true
                        viewModel.toggleComplete(task)
                    },
                    onDeleteTask = viewModel::deleteTask,
                    onToggleHistory = viewModel::toggleShowCompletedHistory, 
                    onStartTask = viewModel::startTask,
                    onPauseTask = viewModel::pauseTask,
                    onResetProgress = viewModel::resetTaskProgress,
                    onSetTime = viewModel::setTaskTargetTime,
                    onDeleteAll = viewModel::deleteAll,
                    onRestoreAll = viewModel::restoreAll,
                    onSetPriority = viewModel::setTaskPriority,
                    onEditTaskTitle = viewModel::updateTaskTitle,
                    onToggleTts = viewModel::toggleTts,
                    onDateSelected = viewModel::onDateSelected,

                    onTaskFinished = { task ->
                        viewModel.onTaskFinished(task)
                    },

                    onStartNextTask = { currentTask ->
                        // Tenta achar a próxima tarefa da lista para começar automaticamente
                        val nextTask = viewModel.findNextTask(currentTask.id)
                        if (nextTask != null) viewModel.startTask(nextTask)
                    }
                )
            )

            // Se a variável for 'true', desenha a animação de celebração por cima de tudo
            if (showConfetti.value) {
                LottieCelebration(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f), // Garante que fica na frente de tudo
                    onFinished = { showConfetti.value = false }
                )
            }
        }
    }
}
