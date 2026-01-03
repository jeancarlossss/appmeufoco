package com.jeancarlos.tasklist.glance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jeancarlos.tasklist.MainActivity
import com.jeancarlos.tasklist.data.TaskDatabase
import com.jeancarlos.tasklist.ui.theme.AppDimens
import kotlinx.coroutines.flow.firstOrNull

// Chaves para armazenar dados nas preferências do Glance
val NEXT_TASK_ID_KEY = intPreferencesKey("next_task_id")
val NEXT_TASK_NAME_KEY = stringPreferencesKey("next_task_name")
val NEXT_TASK_TIME_KEY = longPreferencesKey("next_task_time")

class TaskWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { // Usa o tema do sistema (Material You)
                val prefs = currentState<Preferences>()
                TaskWidgetContent(prefs, context)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun TaskWidgetContent(prefs: Preferences, context: Context) {
        val taskId = prefs[NEXT_TASK_ID_KEY]
        val taskName = prefs[NEXT_TASK_NAME_KEY]
        val totalTime = prefs[NEXT_TASK_TIME_KEY] ?: 0L

        // Intent padrão para abrir o app
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Intent para "Adicionar Tarefa"
        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("OPEN_ADD_TASK", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(16.dp)
                .clickable(actionStartActivity(mainIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (taskName != null && taskId != null) {
                    Text(
                        text = "PRÓXIMO FOCO",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.padding(top = 8.dp)
                    ) {
                        CheckBox(
                            checked = false,
                            onCheckedChange = actionRunCallback<CompleteTaskAction>(
                                actionParametersOf(taskIdKey to taskId)
                            ),
                            colors = CheckboxDefaults.colors(
                                checkedColor = GlanceTheme.colors.primary,
                                uncheckedColor = GlanceTheme.colors.primary
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = taskName,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimens.fontSizeMedium
                            )
                        )
                    }

                    if (totalTime > 0) {
                        Text(
                            text = "⏱ ${formatWidgetDuration(totalTime)} estimados",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Tudo feito! 🎉",
                        style = TextStyle(
                            // CORREÇÃO: Usando Color diretamente para evitar o erro de API restrita (ColorProviderKt)
                            color = ColorProvider(Color(0xFF4CAF50)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Toque para adicionar",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        modifier = GlanceModifier
                            .padding(top = 8.dp)
                            .clickable(actionStartActivity(addIntent))
                    )
                }
            }
        }
    }

    private fun formatWidgetDuration(millis: Long): String {
        val minutesTotal = millis / 1000 / 60
        val hours = minutesTotal / 60
        val minutes = minutesTotal % 60

        return if (hours > 0) "${hours}h ${minutes}m" else "${minutesTotal}min"
    }

    companion object {
        val taskIdKey = ActionParameters.Key<Int>("taskId")

        suspend fun updateWidgetData(context: Context, glanceId: GlanceId) {
            val db = TaskDatabase.getDatabase(context)
            val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
            val nextTask = tasks.firstOrNull { !it.isCompleted }

            updateAppWidgetState(context, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    clear()
                    if (nextTask != null) {
                        this[NEXT_TASK_ID_KEY] = nextTask.id
                        this[NEXT_TASK_NAME_KEY] = nextTask.name
                        this[NEXT_TASK_TIME_KEY] = nextTask.totalTime
                    }
                }
            }
            TaskWidget().update(context, glanceId)
        }

        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(TaskWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateWidgetData(context, glanceId)
            }
        }
    }
}

/**
 * Ação executada quando o usuário marca o CheckBox no Widget.
 */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskWidget.taskIdKey]
        if (taskId != null) {
            val db = TaskDatabase.getDatabase(context)
            // Aqui buscamos a tarefa específica e marcamos como concluída
            val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
            val taskToComplete = tasks.find { it.id == taskId }

            taskToComplete?.let {
                db.taskDao().insertTask(it.copy(isCompleted = true)) // Atualiza no banco
            }

            // Atualiza os dados do widget imediatamente
            TaskWidget.updateWidgetData(context, glanceId)
        }
    }
}