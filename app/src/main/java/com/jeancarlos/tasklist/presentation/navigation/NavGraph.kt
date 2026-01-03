package com.jeancarlos.tasklist.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jeancarlos.tasklist.MainActivity
import com.jeancarlos.tasklist.ViewModelFactory
import com.jeancarlos.tasklist.presentation.TaskListScreenContent
import com.jeancarlos.tasklist.presentation.TaskListViewModel
import com.jeancarlos.tasklist.presentation.screens.HistoryScreen
import com.jeancarlos.tasklist.presentation.screens.StatsScreen
import com.jeancarlos.tasklist.presentation.viewmodels.StatsViewModel
import kotlinx.serialization.Serializable

@Serializable
object TaskListRoute

@Serializable
object StatsRoute

@Serializable
object HistoryRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    factory: ViewModelFactory
) {
    // Pegamos a MainActivity como escopo para compartilhar o ViewModel que recebe os Intents
    val activity = LocalContext.current as MainActivity
    
    NavHost(
        navController = navController,
        startDestination = TaskListRoute
    ) {
        composable<TaskListRoute> {
            // Vinculamos este ViewModel à Activity
            val viewModel: TaskListViewModel = viewModel(viewModelStoreOwner = activity, factory = factory)
            
            TaskListScreenContent(
                viewModel = viewModel,
                onNavigateToStats = {
                    navController.navigate(StatsRoute) { launchSingleTop = true }
                },
                onNavigateToHistory = {
                    navController.navigate(HistoryRoute) { launchSingleTop = true }
                }
            )
        }
        
        composable<StatsRoute> {
            val statsViewModel: StatsViewModel = viewModel(factory = factory)
            StatsScreen(
                stats = statsViewModel.statsState.collectAsState().value,
                isLoading = statsViewModel.isLoading.collectAsState().value,
                onBack = { navController.popBackStack() }
            )
        }

        composable<HistoryRoute> {
            // Compartilha o mesmo ViewModel da Activity para ações de restauração
            val viewModel: TaskListViewModel = viewModel(viewModelStoreOwner = activity, factory = factory)
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
