package com.jeancarlos.tasklist.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeancarlos.tasklist.data.DailyCompletionStat
import com.jeancarlos.tasklist.data.Priority
import com.jeancarlos.tasklist.data.PriorityStat
import com.jeancarlos.tasklist.data.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ProductivityStats(
    val totalFocusedTimeMillis: Long = 0L,
    val timeSpentByPriority: Map<String, Long> = emptyMap(),
    val completedTasksByDay: Map<String, Int> = emptyMap()
)

class StatsViewModel(private val taskDao: TaskDao) : ViewModel() {

    private val _statsState = MutableStateFlow(ProductivityStats())
    val statsState = _statsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false) 
    val isLoading = _isLoading.asStateFlow()

    private var hasLoadedOnce = false

    init {
        loadProductivityStats()
    }

    fun loadProductivityStats() {
        // Removemos hasLoadedOnce para garantir atualização ao voltar para a tela
        // if (hasLoadedOnce) return 

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val totalTime = taskDao.getTotalFocusedTimeCompleted() ?: 0L

                val priorityList: List<PriorityStat> = taskDao.getTimeSpentByPriority()
                val timeByPriorityMap: Map<String, Long> = priorityList.associate {
                    Priority.fromValue(it.priority).name to it.totalTime
                }

                val historyList: List<DailyCompletionStat> = taskDao.getLastSevenDaysCompletionCount()
                // Mapa original do banco
                val rawHistoryMap = historyList.associate { it.completionDate to it.count }
                
                // Geração dos últimos 7 dias completos (com zero onde não há dados)
                val fullHistoryMap = mutableMapOf<String, Int>()
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val today = LocalDate.now()
                
                // Itera dos últimos 6 dias até hoje
                for (i in 6 downTo 0) {
                    val date = today.minusDays(i.toLong())
                    val dateKey = date.format(dateFormatter)
                    // Pega do banco ou assume 0
                    fullHistoryMap[dateKey] = rawHistoryMap[dateKey] ?: 0
                }

                _statsState.value = ProductivityStats(
                    totalFocusedTimeMillis = totalTime,
                    timeSpentByPriority = timeByPriorityMap,
                    completedTasksByDay = fullHistoryMap
                )
                hasLoadedOnce = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
