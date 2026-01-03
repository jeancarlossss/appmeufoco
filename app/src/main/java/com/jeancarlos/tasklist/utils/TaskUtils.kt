package com.jeancarlos.tasklist.utils

import java.util.Locale

/**
 * Formata o tempo para exibição no cronômetro do card (ex: 01:30:15 ou 30:15)
 */
fun formatTimeDisplay(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formata o tempo para rótulos e estatísticas (ex: 1h 30m ou 30m 15s)
 */
fun formatTimeLabel(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%dh %02dm", hours, minutes)
    } else {
        String.format(Locale.US, "%02dm %02ds", minutes, seconds)
    }
}
