package com.jeancarlos.tasklist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jeancarlos.tasklist.data.TaskDao
import com.jeancarlos.tasklist.presentation.TaskListViewModel
import com.jeancarlos.tasklist.presentation.viewmodels.StatsViewModel
import com.jeancarlos.tasklist.utils.NotificationScheduler

class ViewModelFactory(
    private val application: Application,
    private val taskDao: TaskDao,
    private val scheduler: NotificationScheduler
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TaskListViewModel::class.java) -> {
                TaskListViewModel(application, taskDao, scheduler) as T
            }
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> {
                StatsViewModel(taskDao) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}