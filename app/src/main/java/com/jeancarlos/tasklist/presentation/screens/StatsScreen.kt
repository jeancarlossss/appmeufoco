package com.jeancarlos.tasklist.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeancarlos.tasklist.presentation.components.LoadingSkeleton
import com.jeancarlos.tasklist.presentation.viewmodels.ProductivityStats
import com.jeancarlos.tasklist.ui.theme.AppDimens
import com.jeancarlos.tasklist.utils.formatTimeLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE_PARSER = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val DAY_NAME_FORMATTER = SimpleDateFormat("EEE", Locale.forLanguageTag("pt-BR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: ProductivityStats,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Meu Desempenho",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            "Sua jornada de foco",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LoadingSkeleton()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = AppDimens.spacingLarge)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spacingLarge))

            MainFocusCard(totalTimeMillis = stats.totalFocusedTimeMillis)

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader("Produtividade por Prioridade")
            PriorityDistributionChartPlaceholder(stats.timeSpentByPriority)

            Spacer(modifier = Modifier.height(28.dp))

            SectionHeader("Histórico Semanal")
            DailyCompletionChartPlaceholder(stats.completedTasksByDay)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MainFocusCard(totalTimeMillis: Long) {
    val hours = totalTimeMillis / (1000 * 60 * 60)
    
    val (icon, message) = when {
        hours >= 10 -> Pair(Icons.Default.EmojiEvents, "Lendário! Você é imparável.")
        hours >= 5 -> Pair(Icons.Default.LocalFireDepartment, "Incendiário! Continue assim.")
        hours >= 1 -> Pair(Icons.Default.AccessTime, "Bom começo! Mantenha o foco.")
        else -> Pair(Icons.Default.AccessTime, "Cada minuto conta. Vamos lá!")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.iconSizeMedium),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.width(AppDimens.spacingSmall))
                        Text(
                            "Tempo Total de Foco",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(Modifier.height(AppDimens.spacingMedium))
                
                Text(
                    formatTimeLabel(totalTimeMillis),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                
                Spacer(Modifier.height(AppDimens.spacingSmall))
                
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = AppDimens.spacingMedium, start = AppDimens.spacingExtraSmall)
    )
}

@Composable
fun PriorityDistributionChartPlaceholder(data: Map<String, Long>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(AppDimens.borderWidthExtraSmall, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val totalTime = data.values.sum()

            if (totalTime == 0L) {
                Text("Sem dados disponíveis", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                return@Column
            }

            data.entries.sortedByDescending { it.value }.forEach { (priority, time) ->
                val percentage = (time.toFloat() / totalTime.toFloat()) * 100
                
                val priorityLabel = when (priority) {
                    "HIGH" -> "Alta"
                    "MEDIUM" -> "Média"
                    "LOW" -> "Baixa"
                    else -> priority
                }

                val displayColor = when (priority) {
                    "HIGH" -> Color(0xFFEF5350)
                    "MEDIUM" -> Color(0xFFFFB300)
                    "LOW" -> Color(0xFF66BB6A)
                    else -> Color.Gray
                }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.spacingSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(priorityLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("${percentage.toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(AppDimens.heightSmall))
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(AppDimens.spacingMedium).clip(RoundedCornerShape(AppDimens.heightSmall)),
                        color = displayColor,
                        trackColor = displayColor.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyCompletionChartPlaceholder(data: Map<String, Int>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(AppDimens.borderWidthExtraSmall, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        val maxCount = (data.values.maxOrNull() ?: 0).coerceAtLeast(5)
        val todayStr = remember { DATE_PARSER.format(Date()) }
        
        var animationPlayed by remember { mutableStateOf(false) }
        
        LaunchedEffect(key1 = true) {
            animationPlayed = true
        }
        
        Column(modifier = Modifier.padding(top = 24.dp, bottom = AppDimens.spacingLarge, start = AppDimens.spacingMedium, end = AppDimens.spacingMedium)) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.spacingExtraSmall),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.entries.sortedBy { it.key }.forEach { (dateKey, count) ->
                        val isToday = dateKey == todayStr
                        val targetHeight = ((count.toFloat() / maxCount.toFloat()) * 160)
                        
                        val animatedHeight by animateFloatAsState(
                            targetValue = if (animationPlayed) targetHeight else 0f,
                            animationSpec = tween(durationMillis = 800, delayMillis = 100),
                            label = "barHeight"
                        )
                        
                        val dayName = remember(dateKey) { getDayName(dateKey) }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight().weight(1f)
                        ) {
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            } else {
                                Spacer(Modifier.height(AppDimens.iconSizeExtraSmall))
                            }
                            
                            Spacer(Modifier.height(AppDimens.spacingExtraSmall))
                            
                            Box(
                                modifier = Modifier
                                    .width(AppDimens.spacingLarge)
                                    .height(animatedHeight.coerceAtLeast(4f).dp)
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(
                                        if (count > 0) {
                                            Brush.verticalGradient(
                                                colors = if (isToday) {
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                                } else {
                                                    listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                                                }
                                            )
                                        } else {
                                            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)))
                                        }
                                    )
                            )
                            
                            Spacer(Modifier.height(10.dp))
                            
                            Text(
                                text = dayName.take(1),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getDayName(dateString: String): String {
    return try {
        val date = DATE_PARSER.parse(dateString)
        date?.let { DAY_NAME_FORMATTER.format(it).uppercase().replace(".", "") } ?: dateString.takeLast(2)
    } catch (_: Exception) {
        dateString.takeLast(2)
    }
}
