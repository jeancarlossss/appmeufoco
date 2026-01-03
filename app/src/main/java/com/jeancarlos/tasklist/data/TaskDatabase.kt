package com.jeancarlos.tasklist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Esta classe é o "Coração" do banco de dados.
 * Ela conecta as nossas tabelas (Entities) com as funções de acesso (DAOs).
 * A versão aumentou para 2 porque adicionamos campos novos recentemente.
 */
@Database(entities = [TaskItem::class], version = 2, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {

    // Aqui a gente avisa ao banco que ele deve usar o TaskDao
    abstract fun taskDao(): TaskDao

    companion object {
        // @Volatile garante que a mudança nessa variável seja vista por todos os processos do app na hora.
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /**
         * Este método serve para pegar a instância do banco de dados.
         * Usamos o padrão "Singleton" para garantir que o app só crie UM banco de dados,
         * economizando memória e evitando conflitos.
         */
        fun getDatabase(context: Context): TaskDatabase {
            // Se já existir um banco, a gente retorna ele. Se não, a gente cria um novo.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    // Se a gente mudar a estrutura do banco, ele apaga o antigo e cria um novo 
                    // (útil durante o desenvolvimento para não dar erro de migração).
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
