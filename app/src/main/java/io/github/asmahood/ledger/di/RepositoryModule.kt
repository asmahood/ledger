package io.github.asmahood.ledger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.asmahood.ledger.data.csv.ContentResolverCsvFileReader
import io.github.asmahood.ledger.data.csv.CsvFileReader
import io.github.asmahood.ledger.data.repository.CategoryRepository
import io.github.asmahood.ledger.data.repository.OfflineCategoryRepository
import io.github.asmahood.ledger.data.repository.OfflineTransactionRepository
import io.github.asmahood.ledger.data.repository.TransactionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindCategoryRepository(impl: OfflineCategoryRepository): CategoryRepository

    @Singleton
    @Binds
    abstract fun bindTransactionRepository(impl: OfflineTransactionRepository): TransactionRepository

    @Singleton
    @Binds
    abstract fun bindCsvFileReader(impl: ContentResolverCsvFileReader): CsvFileReader
}