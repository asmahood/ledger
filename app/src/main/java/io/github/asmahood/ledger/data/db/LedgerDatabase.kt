package io.github.asmahood.ledger.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [], version = 1, exportSchema = true)
abstract class LedgerDatabase : RoomDatabase() {}