package com.jeancarlos.tasklist.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jeancarlos.tasklist.MainActivity
import com.jeancarlos.tasklist.R
import com.jeancarlos.tasklist.TaskApplication

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Tarefa"

        if (taskId != -1) {
            showNotification(context, taskId, taskName)
        }
    }

    private fun showNotification(context: Context, taskId: Int, taskName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // NOVO: Passa o ID da tarefa para a Activity saber qual diálogo abrir
            putExtra("FINISHED_TASK_ID", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TaskApplication.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Tempo Esgotado! \uD83C\uDF89")
            .setContentText("A tarefa '$taskName' foi finalizada.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()

        notificationManager.notify(taskId, notification)
    }
}
