package com.jeancarlos.tasklist.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeancarlos.tasklist.data.Priority
import com.jeancarlos.tasklist.data.TaskDao
import com.jeancarlos.tasklist.data.TaskItem
import com.jeancarlos.tasklist.glance.TaskWidget
import com.jeancarlos.tasklist.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Este objeto guarda tudo que a tela precisa mostrar de uma vez só.
 * É como uma "foto" do estado atual do app.
 */
data class TaskListUiState(
    val tasks: List<TaskItem> = emptyList(), // Lista de tarefas que aparecem
    val newTaskText: String = "",           // Texto que o usuário está digitando
    val showCompletedHistory: Boolean = false, // Mostrar tarefas terminadas ou não
    val isTtsEnabled: Boolean = true,       // Voz do Google (Text-to-Speech) ligada?
    val selectedDate: LocalDate = LocalDate.now(), // Dia que o usuário clicou no calendário
    val isLoading: Boolean = true,          // O app ainda está carregando os dados?
    val completedCount: Int = 0,            // Quantas tarefas foram feitas
    val totalCount: Int = 0                 // Total de tarefas do dia
) {
    // Calcula a porcentagem da barra de progresso (0.0 a 1.0)
    val progress: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

/**
 * O ViewModel é o "Cérebro" da interface.
 * Ele decide o que acontece quando você clica em um botão e cuida dos dados.
 */
class TaskListViewModel(
    private val application: Application,
    private val dao: TaskDao, // Acesso ao banco de dados
    private val scheduler: NotificationScheduler // Alarme/Notificação
) : ViewModel() {

    // States privados: onde o ViewModel guarda as informações internamente
    private val _newTaskText = MutableStateFlow("")
    private val _showCompletedHistory = MutableStateFlow(false)
    private val _isTtsEnabled = MutableStateFlow(true)
    private val _newTaskPriority = MutableStateFlow(Priority.LOW)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    
    // Guarda se alguma tarefa acabou de ser concluída (para mostrar o confete!)
    private val _finishedTask = MutableStateFlow<TaskItem?>(null)
    val finishedTask = _finishedTask.asStateFlow()

    /**
     * O 'uiState' junta todas as informações acima e entrega para a tela desenhar.
     * Ele observa o banco de dados e as mudanças de data automaticamente.
     */
    val uiState: StateFlow<TaskListUiState> = combine(
        dao.getAllTasks(),
        _newTaskText,
        _showCompletedHistory,
        _isTtsEnabled,
        _selectedDate
    ) { allTasks, newText, showHistory, ttsEnabled, selectedDate ->
        
        val today = LocalDate.now()
        
        // Filtra as tarefas: se for o dia de hoje, mostra pendentes. 
        // Se for outro dia, mostra só o que foi concluído naquele dia.
        val tasksForSelectedDate = allTasks.filter { task ->
            if (task.isCompleted) {
                val completedDate = task.completedAt?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: today
                completedDate == selectedDate
            } else {
                selectedDate == today
            }
        }

        val completedCount = tasksForSelectedDate.count { it.isCompleted }
        val totalCount = tasksForSelectedDate.size

        TaskListUiState(
            tasks = tasksForSelectedDate,
            newTaskText = newText,
            showCompletedHistory = showHistory,
            isTtsEnabled = ttsEnabled,
            selectedDate = selectedDate,
            isLoading = false,
            completedCount = completedCount,
            totalCount = totalCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskListUiState()
    )

    // Pega o histórico total de todas as tarefas já feitas na história do app
    val fullHistoryTasks: StateFlow<List<TaskItem>> = dao.getAllTasks()
        .map { tasks -> tasks.filter { it.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Métodos para mudar o estado das coisas:

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateNewTaskText(text: String) {
        _newTaskText.value = text
    }

    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
    }

    // Adiciona uma tarefa nova
    fun addTask() {
        val text = _newTaskText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val newTask = TaskItem(
                    name = text,
                    priority = _newTaskPriority.value.value
                )
                dao.insertTask(newTask)
                _newTaskText.value = "" // Limpa o campo de texto
                _newTaskPriority.value = Priority.LOW
                updateWidget() // Avisa o Widget do celular que mudou
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTaskTitle(task: TaskItem, newTitle: String) {
        if (newTitle.isBlank()) return
        val updated = task.copy(name = newTitle)
        updateTaskInDb(updated)
    }

    fun setTaskPriority(task: TaskItem, priority: Priority) {
        val updated = task.copy(priority = priority.value)
        updateTaskInDb(updated)
    }

    // Marca ou desmarca uma tarefa como feita
    fun toggleComplete(task: TaskItem) {
        scheduler.cancelTaskNotification(task.id) // Cancela o alarme se existir

        val isNowCompleted = !task.isCompleted
        
        // Se estava rodando o cronômetro, a gente pausa e calcula o tempo final
        val taskWithCurrentTime = if (task.isRunning) {
            calculatePausedTask(task)
        } else {
            task
        }
        
        updateTaskInDb(taskWithCurrentTime.copy(
            isCompleted = isNowCompleted,
            completedAt = if (isNowCompleted) System.currentTimeMillis() else null
        ))
    }

    // Inicia o cronômetro da tarefa
    fun startTask(task: TaskItem) {
        if (task.isRunning || task.isCompleted) return

        // Se a tarefa tem um tempo alvo, a gente agenda uma notificação para quando o tempo acabar
        val remainingTime = task.totalTime - task.accumulatedTime
        val triggerTime = System.currentTimeMillis() + remainingTime

        if (task.totalTime > 0 && remainingTime > 0) {
            scheduler.scheduleTaskNotification(task.id, task.name, triggerTime)
        }

        val updated = task.copy(
            isRunning = true,
            lastStartTime = System.currentTimeMillis()
        )
        updateTaskInDb(updated)
    }

    // Pausa o cronômetro
    fun pauseTask(task: TaskItem) {
        if (!task.isRunning) return
        scheduler.cancelTaskNotification(task.id)
        val updated = calculatePausedTask(task)
        updateTaskInDb(updated)
    }

    // Quando o tempo da tarefa chega no limite
    fun onTaskFinished(task: TaskItem) {
        scheduler.cancelTaskNotification(task.id)
        val updated = calculatePausedTask(task)
        updateTaskInDb(updated)
        _finishedTask.value = updated // Isso vai disparar o Confete!
    }

    fun resetTaskProgress(task: TaskItem) {
        scheduler.cancelTaskNotification(task.id)
        val updated = task.copy(isRunning = false, accumulatedTime = 0L, startTime = null)
        updateTaskInDb(updated)
    }

    fun setTaskTargetTime(task: TaskItem, timeInMillis: Long) {
        val updated = task.copy(totalTime = timeInMillis)
        updateTaskInDb(updated)
    }

    // Faz a conta de quanto tempo passou desde que o usuário apertou "Play"
    private fun calculatePausedTask(task: TaskItem): TaskItem {
        val now = System.currentTimeMillis()
        val timeElapsed = now - task.lastStartTime
        var newAccumulatedTime = task.accumulatedTime + timeElapsed
        
        // Não deixa o tempo passar do limite definido pelo usuário
        if (task.totalTime in 1 until newAccumulatedTime) {
            newAccumulatedTime = task.totalTime
        }
        
        return task.copy(
            isRunning = false,
            accumulatedTime = newAccumulatedTime
        )
    }

    // Salva qualquer mudança no banco de dados e atualiza o Widget do Android
    private fun updateTaskInDb(task: TaskItem) {
        viewModelScope.launch { 
            dao.updateTask(task) 
            updateWidget()
        }
    }

    private fun updateWidget() {
        viewModelScope.launch {
            TaskWidget.updateAll(application)
        }
    }

    // Funções extras para deletar, restaurar e limpar estatísticas...
    fun deleteTask(task: TaskItem) {
        scheduler.cancelTaskNotification(task.id)
        viewModelScope.launch { dao.deleteTask(task); updateWidget() }
    }

    fun deleteAll() { viewModelScope.launch { dao.deleteAll(); updateWidget() } }
    fun restoreAll() { viewModelScope.launch { dao.restoreAllTasks(); updateWidget() } }
    fun resetAllTime() { viewModelScope.launch { dao.resetAllTime(); updateWidget() } }
    fun toggleShowCompletedHistory() { _showCompletedHistory.value = !_showCompletedHistory.value }
    fun clearFinishedTask() { _finishedTask.value = null }

    fun findNextTask(currentTaskId: Int): TaskItem? {
        val currentList = uiState.value.tasks
        return currentList.filter { !it.isCompleted }.firstOrNull { it.id != currentTaskId }
    }

    fun setFinishedTaskById(taskId: Int) {
        viewModelScope.launch {
            val allTasks = dao.getAllTasks().firstOrNull()
            val task = allTasks?.find { it.id == taskId }
            if (task != null) {
                _finishedTask.value = task
            }
        }
    }
}
