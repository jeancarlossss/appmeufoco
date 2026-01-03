package com.jeancarlos.tasklist

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jeancarlos.tasklist.presentation.TaskListViewModel
import com.jeancarlos.tasklist.presentation.navigation.NavGraph
import com.jeancarlos.tasklist.ui.theme.TaskListTheme
import com.jeancarlos.tasklist.utils.NotificationScheduler

class MainActivity : ComponentActivity() {

    private val taskListViewModel by viewModels<TaskListViewModel> { 
        val appContainer = application as TaskApplication
        ViewModelFactory(application, appContainer.db.taskDao(), NotificationScheduler(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            TaskListTheme {
                val navController = rememberNavController()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { }
                    )
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        navController = navController,
                        factory = ViewModelFactory(
                            application,
                            (application as TaskApplication).db.taskDao(),
                            NotificationScheduler(this)
                        )
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Tratamento para notificação de tarefa finalizada
        val taskId = intent?.getIntExtra("FINISHED_TASK_ID", -1) ?: -1
        if (taskId != -1) {
            taskListViewModel.setFinishedTaskById(taskId)
        }

        // Tratamento para clique do widget "Adicionar Tarefa"
        // Removido bloco vazio para evitar avisos do compilador. 
        // Trazer a Activity para frente já é o comportamento padrão.
    }
}