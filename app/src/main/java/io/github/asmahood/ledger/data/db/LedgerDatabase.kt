package io.github.asmahood.ledger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = true
)
abstract class LedgerDatabase : RoomDatabase()