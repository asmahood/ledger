package io.github.asmahood.ledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.asmahood.ledger.data.db.LedgerDatabase
import io.github.asmahood.ledger.data.db.dao.CategoryDao
import io.github.asmahood.ledger.data.db.dao.TransactionDao

/**
 * Provides each DAO from the bound [LedgerDatabase]. Kept separate from [DatabaseModule] so the
 * database binding can be replaced in tests (in-memory) while these DAO providers continue to work
 * unchanged. Add new DAOs here as entities are introduced.
 */
@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun provideCategoryDao(database: LedgerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: LedgerDatabase): TransactionDao = database.transactionDao()
}
