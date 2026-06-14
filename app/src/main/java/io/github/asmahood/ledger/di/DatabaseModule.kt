package io.github.asmahood.ledger.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.asmahood.ledger.data.db.LedgerDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase {
        return Room.databaseBuilder(
            context,
            LedgerDatabase::class.java,
            "ledger_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
}