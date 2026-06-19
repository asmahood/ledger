package io.github.asmahood.ledger.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.asmahood.ledger.data.db.LedgerDatabase
import javax.inject.Singleton

/**
 * Replaces only the production [DatabaseModule] with an in-memory database, so each Hilt test runs
 * against a fresh, hermetic Room instance. DAOs still come from the real `DaoModule`, so new DAOs
 * never need to be re-declared here.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Singleton
    @Provides
    fun provideInMemoryDatabase(@ApplicationContext context: Context): LedgerDatabase =
        Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
