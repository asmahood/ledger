package io.github.asmahood.ledger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.asmahood.ledger.data.db.dao.BudgetDao
import io.github.asmahood.ledger.data.db.dao.CategoryDao
import io.github.asmahood.ledger.data.db.dao.TransactionDao
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
}