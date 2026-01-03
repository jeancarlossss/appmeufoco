package com.jeancarlos.tasklist.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jeancarlos.tasklist.data.Priority
import com.jeancarlos.tasklist.data.TaskItem
import com.jeancarlos.tasklist.ui.theme.AppDimens
import com.jeancarlos.tasklist.ui.theme.OrangeAmber
import com.jeancarlos.tasklist.utils.formatTimeDisplay
import com.jeancarlos.tasklist.utils.formatTimeLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: (Long) -> Unit,
    onStartTask: () -> Unit,
    onPauseTask: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onResetProgress: () -> Unit,
    onTimeFinished: () -> Unit,
    onSetPriority: (Priority) -> Unit,
    onEditTitle: (String) -> Unit
) {
    val showSetTimeDialog = remember { mutableStateOf(false) }
    val showPriorityMenu = remember { mutableStateOf(false) }
    val showEditTitleDialog = remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val currentTime = remember(task.isRunning, task.accumulatedTime, task.lastStartTime) {
        mutableLongStateOf(
            if (task.isRunning) {
                task.accumulatedTime + (System.currentTimeMillis() - task.lastStartTime)
            } else {
                task.accumulatedTime
            }
        )
    }

    LaunchedEffect(task.isRunning) {
        if (task.isRunning) {
            val initialAccumulated = task.accumulatedTime
            val startTime = task.lastStartTime
            var effectTriggered = false

            while (isActive && !effectTriggered) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val calculatedTime = initialAccumulated + (now - startTime)
                currentTime.longValue = calculatedTime

                if (task.totalTime in 1..calculatedTime) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTimeFinished()
                    effectTriggered = true
                }
            }
        }
    }

    if (showSetTimeDialog.value) {
        SetTimeDialog(
            initialMinutes = 1,
            onConfirm = { minutes ->
                if (minutes == 0) {
                    onSetTime(0L)
                } else {
                    onSetTime(minutes * 60 * 1000L)
                }
                showSetTimeDialog.value = false
            },
            onDismiss = { showSetTimeDialog.value = false }
        )
    }
    
    if (showEditTitleDialog.value) {
        EditTitleDialog(
            currentTitle = task.name,
            onConfirm = { newTitle ->
                onEditTitle(newTitle)
                showEditTitleDialog.value = false
            },
            onDismiss = { showEditTitleDialog.value = false }
        )
    }

    val priorityColor = when (Priority.fromValue(task.priority)) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> OrangeAmber
        Priority.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.paddingBetweenElements)
            .shadow(
                elevation = if (task.isRunning) AppDimens.spacingSmall else 2.dp,
                shape = RoundedCornerShape(AppDimens.spacingLarge),
                ambientColor = priorityColor.copy(alpha = 0.2f),
                spotColor = priorityColor.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(AppDimens.spacingLarge),
        color = if (task.isCompleted) 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) 
        else 
            MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (task.isRunning) 1.5.dp else 0.5.dp,
            color = if (task.isRunning) priorityColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(AppDimens.spacingExtraSmall)
                    .background(priorityColor)
                    .clickable(enabled = !task.isCompleted) { showPriorityMenu.value = true }
            ) {
                PriorityDropdown(
                    expanded = showPriorityMenu.value,
                    onDismiss = { showPriorityMenu.value = false },
                    onSelect = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetPriority(it)
                        showPriorityMenu.value = false
                    }
                )
            }

            Column(modifier = Modifier.padding(AppDimens.spacingMedium).weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleComplete(currentTime.longValue)
                        },
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (task.isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(AppDimens.spacingMedium))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable(enabled = !task.isCompleted) { showEditTitleDialog.value = true }
                        )
                        
                        if (task.isCompleted) {
                            val completionTime = task.completedAt?.let { 
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) 
                            } ?: "--:--"
                            Text(
                                text = "Concluída às $completionTime • Focado por ${formatTimeLabel(task.accumulatedTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    if (!task.isCompleted) {
                        val priorityLabel = when (Priority.fromValue(task.priority)) {
                            Priority.HIGH -> "Alta"
                            Priority.MEDIUM -> "Média"
                            Priority.LOW -> "Baixa"
                        }
                        
                        Surface(
                            color = priorityColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(AppDimens.spacingSmall),
                            modifier = Modifier.clickable { showPriorityMenu.value = true }
                        ) {
                            Text(
                                text = priorityLabel,
                                modifier = Modifier.padding(horizontal = AppDimens.spacingSmall, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(!task.isCompleted) {
                    Column(modifier = Modifier.padding(top = AppDimens.spacingMedium)) {
                        if (task.totalTime > 0) {
                            val progress by animateFloatAsState(
                                targetValue = (currentTime.longValue.toFloat() / task.totalTime.toFloat()).coerceIn(0f, 1f),
                                label = "progress"
                            )
                            
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppDimens.heightSmall)
                                    .clip(RoundedCornerShape(AppDimens.cornerRadiusSmall)),
                                color = priorityColor,
                                trackColor = priorityColor.copy(alpha = 0.1f)
                            )
                            Spacer(Modifier.height(AppDimens.spacingSmall))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeDisplay(
                                currentTime = currentTime.longValue,
                                totalTime = task.totalTime,
                                isRunning = task.isRunning,
                                onClick = { showSetTimeDialog.value = true }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onResetProgress,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(AppDimens.spacingSmall))

                                FloatingActionButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (task.isRunning) onPauseTask(currentTime.longValue) else onStartTask()
                                    },
                                    containerColor = if (task.isRunning) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.primary,
                                    contentColor = if (task.isRunning) 
                                        MaterialTheme.colorScheme.onSecondaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                                ) {
                                    Icon(
                                        imageVector = if (task.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Priority) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = { Text("Alta") },
            leadingIcon = { Icon(Icons.Default.PriorityHigh, null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onSelect(Priority.HIGH) }
        )
        DropdownMenuItem(
            text = { Text("Média") },
            leadingIcon = { Icon(Icons.Default.Remove, null, tint = OrangeAmber) },
            onClick = { onSelect(Priority.MEDIUM) }
        )
        DropdownMenuItem(
            text = { Text("Baixa") },
            leadingIcon = { Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { onSelect(Priority.LOW) }
        )
    }
}

@Composable
fun TimeDisplay(
    currentTime: Long,
    totalTime: Long,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.spacingSmall))
            .clickable { onClick() }
            .padding(AppDimens.spacingExtraSmall)
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.iconSizeSmall),
            tint = if (isRunning) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(Modifier.width(AppDimens.heightSmall))
        Text(
            text = formatTimeDisplay(currentTime),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (totalTime > 0) {
            Text(
                text = " / ${formatTimeDisplay(totalTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetTimeDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val minutes = remember { mutableIntStateOf(initialMinutes.coerceIn(1, 359)) }
    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Timer, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(AppDimens.spacingSmall))
                    Text(
                        "Duração da Tarefa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                CircularTimePicker(
                    value = minutes.intValue,
                    onValueChange = { minutes.intValue = it },
                    maxMinutes = 359,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(0)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sem limite de tempo", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onConfirm(minutes.intValue.coerceIn(1, 359)) 
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .width(120.dp)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditTitleDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val text = remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Tarefa") },
        text = {
            OutlinedTextField(
                value = text.value,
                onValueChange = { text.value = it },
                label = { Text("Nome da tarefa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.cornerRadiusMedium)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.value) }, shape = RoundedCornerShape(AppDimens.cornerRadiusMedium)) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
