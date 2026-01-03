package com.jeancarlos.tasklist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aqui definimos as Prioridades das tarefas.
 * Cada nível tem um valor numérico para ajudar na hora de ordenar no banco de dados.
 */
enum class Priority(val value: Int) {
    HIGH(3),   // Alta - Valor 3
    MEDIUM(2), // Média - Valor 2
    LOW(1);    // Baixa - Valor 1

    companion object {
        // Função para converter um número de volta para o nome da prioridade
        fun fromValue(value: Int) = entries.find { it.value == value } ?: LOW
    }
}

/**
 * Estas "Data Classes" são como pequenos containers de dados.
 * Elas não são tabelas do banco, mas servem para o Room organizar o resultado de algumas contas (estatísticas).
 */

// Usada para contar quantas tarefas terminamos em cada dia
data class DailyCompletionStat(
    val completionDate: String,
    val count: Int
)

// Usada para somar o tempo que passamos em cada nível de prioridade
data class PriorityStat(
    val priority: Int,
    val totalTime: Long
)

/**
 * Esta é a nossa Entidade. No mundo do Room, isso significa que ela VIRA uma tabela no banco de dados.
 * O nome da tabela será "task_item".
 */
@Entity(tableName = "task_item")
data class TaskItem(
    // O ID é gerado automaticamente pelo banco (1, 2, 3...)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val name: String, // Nome da tarefa
    val isCompleted: Boolean = false, // Se já foi terminada
    val startTime: Long? = null, // Quando o cronômetro começou
    val elapsedTime: Long = 0L, // Tempo que passou rodando agora
    val totalTime: Long = 0L, // Tempo total estimado (se houver)
    val accumulatedTime: Long = 0L, // Soma de todo o tempo focado nessa tarefa
    val isRunning: Boolean = false, // O cronômetro está rodando agora?
    val lastStartTime: Long = 0L, // Última vez que demos "play"
    val priority: Int = Priority.LOW.value, // Qual a prioridade (padrão é Baixa)
    val completedAt: Long? = null // Guarda o horário exato que terminamos a tarefa (timestamp)
)
