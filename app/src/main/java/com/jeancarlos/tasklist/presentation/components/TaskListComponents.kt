package com.jeancarlos.tasklist.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeancarlos.tasklist.ui.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    isTtsEnabled: Boolean,
    onToggleTts: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Column {
                Text(
                    text = "Foco Diário",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Organize suas ideias",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleTts) {
                Icon(
                    imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = if (isTtsEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "Histórico", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onNavigateToStats) {
                Icon(Icons.Default.Insights, contentDescription = "Estatísticas", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun TaskInputSection(
    visible: Boolean,
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(modifier = Modifier.padding(bottom = AppDimens.spacingLarge)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.cornerRadiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(AppDimens.borderWidthExtraSmall, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(AppDimens.spacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Qual o seu próximo foco?", fontSize = AppDimens.fontSizeSmall) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAdd() })
                    )

                    IconButton(
                        onClick = onAdd,
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .padding(AppDimens.spacingExtraSmall)
                            .background(
                                if (text.isNotBlank()) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(AppDimens.cornerRadiusMedium)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    allTasksCount: Int,
    completedTasks: Int,
    totalTasks: Int
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (allTasksCount > 0 && completedTasks == totalTasks) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(AppDimens.spacingLarge))
                Text("Tudo pronto por enquanto!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Você concluiu todos os seus focos.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                Icon(Icons.Default.RocketLaunch, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                Spacer(Modifier.height(AppDimens.spacingLarge))
                Text("No que vamos focar agora?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun CompletedHeader(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cornerRadiusSmall))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = AppDimens.spacingSmall, horizontal = AppDimens.spacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Concluídas ($count)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProgressFooter(
    progressPercent: Int,
    completedTasks: Int,
    totalTasks: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.spacingLarge),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Progresso do Dia",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(AppDimens.spacingExtraSmall))
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(AppDimens.spacingExtraSmall)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
            Spacer(Modifier.width(AppDimens.spacingLarge))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$progressPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "$completedTasks/$totalTasks feitos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}