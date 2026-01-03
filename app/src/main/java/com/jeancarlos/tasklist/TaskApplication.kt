package com.jeancarlos.tasklist

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.jeancarlos.tasklist.data.TaskDatabase

class TaskApplication : Application() {
    lateinit var db: TaskDatabase

    companion object {
        // Mudança no ID para garantir que o sistema crie um novo canal sem som
        const val CHANNEL_ID = "task_timer_channel_v2"
    }

    override fun onCreate() {
        super.onCreate()
        db = TaskDatabase.getDatabase(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Notificações de Tarefa"
        val descriptionText = "Avisos quando o tempo de uma tarefa acaba"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            // Desativa o som padrão do sistema para este canal
            setSound(null, null)
            enableVibration(true)
        }
        
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Remove o canal antigo se existir
        notificationManager.deleteNotificationChannel("task_timer_channel")
        
        notificationManager.createNotificationChannel(channel)
    }
}
