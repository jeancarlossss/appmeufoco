package com.jeancarlos.tasklist.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.*

@Composable
fun CircularTimePicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxMinutes: Int = 359
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Configurações do arco
    val startAngle = -85f 
    val sweepAngle = 350f 

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentMaxMinutes by rememberUpdatedState(maxMinutes)
    val currentValue by rememberUpdatedState(value)

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> 
                            handleTouch(offset, size.toSize(), startAngle, sweepAngle, currentMaxMinutes, currentValue, haptic, currentOnValueChange) 
                        },
                        onDrag = { change, _ -> 
                            handleTouch(change.position, size.toSize(), startAngle, sweepAngle, currentMaxMinutes, currentValue, haptic, currentOnValueChange)
                            change.consume()
                        }
                    )
                }
        ) {
            val canvasSize = size
            val radius = (min(canvasSize.width, canvasSize.height) / 2) - 25.dp.toPx()
            val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
            
            // 1. Desenha marcações de escala (Ticks)
            val tickCount = 12 
            for (i in 0..tickCount) {
                val tickAngle = startAngle + (i.toFloat() / tickCount) * sweepAngle
                val angleRad = Math.toRadians(tickAngle.toDouble())
                val innerRadius = radius - 15.dp.toPx()
                val outerRadius = radius - 5.dp.toPx()
                
                val start = Offset(
                    center.x + innerRadius * cos(angleRad).toFloat(),
                    center.y + innerRadius * sin(angleRad).toFloat()
                )
                val end = Offset(
                    center.x + outerRadius * cos(angleRad).toFloat(),
                    center.y + outerRadius * sin(angleRad).toFloat()
                )
                
                drawLine(
                    color = if (value >= (i * 30)) primaryColor.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 2. Track (Fundo do arco)
            drawArc(
                color = Color.LightGray.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // 3. Progresso preenchido com Gradiente
            val progressPercentage = ((value.toFloat() - 1) / (maxMinutes - 1)).coerceIn(0f, 1f)
            val progressSweep = progressPercentage * sweepAngle
            
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to primaryColor.copy(alpha = 0.6f),
                    progressPercentage to primaryColor,
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = progressSweep,
                useCenter = false,
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            
            // 4. Thumb (Bolinha)
            val thumbAngle = Math.toRadians((startAngle + progressSweep).toDouble())
            val thumbX = center.x + radius * cos(thumbAngle).toFloat()
            val thumbY = center.y + radius * sin(thumbAngle).toFloat()
            
            // Sombra do thumb
            drawCircle(
                color = Color.Black.copy(alpha = 0.1f),
                radius = 16.dp.toPx(),
                center = Offset(thumbX, thumbY + 2.dp.toPx())
            )
            
            // Corpo branco do thumb
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
            
            // Miolo colorido
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
        
        // Elementos centrais
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { if (value > 1) onValueChange(value - 1) }, // Agora de 1 em 1
                    modifier = Modifier
                        .size(44.dp)
                        .background(primaryColor.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = value.toString(),
                    fontSize = if (value >= 100) 48.sp else 58.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = { if (value < maxMinutes) onValueChange(value + 1) }, // Agora de 1 em 1
                    modifier = Modifier
                        .size(44.dp)
                        .background(primaryColor.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = "minutos",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
    }
}

private fun handleTouch(
    pos: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    maxMinutes: Int,
    currentValue: Int,
    haptic: HapticFeedback,
    onValueChange: (Int) -> Unit
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val x = pos.x - centerX
    val y = pos.y - centerY
    
    val angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    
    var relativeAngle = (angle - startAngle)
    while (relativeAngle < 0) relativeAngle += 360f
    relativeAngle %= 360f
    
    val newValue = if (relativeAngle > sweepAngle) {
        if (relativeAngle > sweepAngle + (360 - sweepAngle) / 2) 1 else maxMinutes
    } else {
        ((relativeAngle / sweepAngle) * (maxMinutes - 1)).toInt() + 1
    }
    
    val finalValue = newValue.coerceIn(1, maxMinutes)

    val diff = abs(finalValue - currentValue)
    if (diff < maxMinutes / 2) {
        if (finalValue != currentValue) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(finalValue)
        }
    }
}
