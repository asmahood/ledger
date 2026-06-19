package io.github.asmahood.ledger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.data.db.LedgerDatabase
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OfflineCategoryRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var repository: OfflineCategoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineCategoryRepository(db.categoryDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertCategory_persistsAndMapsBackToDomainModel() = runBlocking {
        repository.insertCategory(
            Category(id = 0, name = "Groceries", type = TransactionType.EXPENSE, description = "Food"),
        )

        val all = repository.getAlLCategoriesStream().first()
        assertEquals(1, all.size)
        val stored = all.first()
        assertEquals("Groceries", stored.name)
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals("Food", stored.description)
    }

    @Test(expected = DuplicateCategoryException::class)
    fun insertCategory_duplicateName_throwsDuplicateCategoryException() = runBlocking {
        repository.insertCategory(Category(id = 0, name = "Groceries", type = TransactionType.EXPENSE))
        // Translated from the underlying SQLiteConstraintException by the repository.
        repository.insertCategory(Category(id = 0, name = "Groceries", type = TransactionType.INCOME))
        Unit
    }
}
