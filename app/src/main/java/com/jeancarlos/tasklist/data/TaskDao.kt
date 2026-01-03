package com.jeancarlos.tasklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Esta interface é o nosso DAO (Data Access Object). 
 * Basicamente, é aqui que a gente define como o aplicativo vai "conversar" com o banco de dados.
 * É como se fosse um menu de comandos que o Room vai traduzir para SQL.
 */
@Dao
interface TaskDao {

    // Pega todas as tarefas do banco. 
    // O Room usa o "Flow" para que, sempre que algo mudar no banco, a lista seja atualizada sozinha na tela.
    // A gente ordena por prioridade (as mais altas primeiro) e depois por ID (as mais recentes).
    @Query("SELECT * FROM task_item ORDER BY priority DESC, id DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    // Insere uma nova tarefa. Se a tarefa já existir (mesmo ID), ele substitui (REPLACE).
    // Usamos "suspend" porque operações de banco de dados demoram um pouco e não podem travar o app.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem)

    // Atualiza os dados de uma tarefa que já existe.
    @Update
    suspend fun updateTask(task: TaskItem)

    // Deleta uma tarefa específica da lista.
    @Delete
    suspend fun deleteTask(task: TaskItem)

    // Deleta TUDO da tabela de tarefas. Cuidado com esse aqui!
    @Query("DELETE FROM task_item")
    suspend fun deleteAll()

    // "Desmarca" todas as tarefas concluídas, voltando o tempo e o status de completado para o padrão.
    @Query("UPDATE task_item SET isCompleted = 0, accumulatedTime = 0, completedAt = null WHERE isCompleted = 1")
    suspend fun restoreAllTasks()

    // Zera o cronômetro de todas as tarefas do app.
    @Query("UPDATE task_item SET elapsedTime = 0, startTime = null, accumulatedTime = 0, isRunning = 0")
    suspend fun resetAllTime()

    // --- MÉTODOS PARA ESTATÍSTICAS (A parte que gera os gráficos e relatórios) ---

    // 1. Soma todo o tempo que a gente passou focado nas tarefas que já terminamos.
    @Query("SELECT SUM(accumulatedTime) FROM task_item WHERE isCompleted = 1")
    suspend fun getTotalFocusedTimeCompleted(): Long?

    // 2. Agrupa o tempo gasto de acordo com a prioridade (ex: quanto tempo gastei em tarefas "Altas").
    @Query("""
        SELECT priority, SUM(accumulatedTime) as totalTime
        FROM task_item
        WHERE isCompleted = 1 GROUP BY priority
    """)
    suspend fun getTimeSpentByPriority(): List<PriorityStat>

    // 3. Pega quantas tarefas a gente terminou em cada um dos últimos 7 dias.
    // O Room faz uma mágica com 'strftime' para transformar o tempo (timestamp) em uma data legível (ano-mes-dia).
    @Query("""
        SELECT strftime('%Y-%m-%d', completedAt/1000, 'unixepoch', 'localtime') AS completionDate, COUNT(id) as count
        FROM task_item 
        WHERE isCompleted = 1 AND completedAt IS NOT NULL
        GROUP BY completionDate
        ORDER BY completionDate DESC
        LIMIT 7
    """)
    suspend fun getLastSevenDaysCompletionCount(): List<DailyCompletionStat>
}
