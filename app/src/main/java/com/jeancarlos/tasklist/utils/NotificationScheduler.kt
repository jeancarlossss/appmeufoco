package com.jeancarlos.tasklist.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskNotification(taskId: Int, taskName: String, triggerAtMillis: Long) {
        // Validação: Não agendar se o tempo já passou
        if (triggerAtMillis <= System.currentTimeMillis()) return

        // Verifica permissão para Alarmes Exatos no Android 12+ (S)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("NotificationScheduler", "Permissão para alarmes exatos não concedida.")
                // Aqui poderíamos pedir a permissão, mas vamos focar no básico primeiro
                return
            }
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_NAME", taskName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId, // ID único para cada tarefa
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d("NotificationScheduler", "Alarme agendado para tarefa $taskId em $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e("NotificationScheduler", "Erro ao agendar alarme: ${e.message}")
        }
    }

    fun cancelTaskNotification(taskId: Int) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("NotificationScheduler", "Alarme cancelado para tarefa $taskId")
    }
}