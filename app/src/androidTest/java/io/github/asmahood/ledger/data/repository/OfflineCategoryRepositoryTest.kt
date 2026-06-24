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
import org.junit.Assert.assertNull
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
        repository = OfflineCategoryRepository(db, db.categoryDao(), db.budgetDao())
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

        val all = repository.getAllCategoriesStream().first()
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

    @Test
    fun setBudget_setsUpdatesAndClearsThroughCategoryStream() = runBlocking {
        val id = repository.insertCategory(
            Category(id = 0, name = "Groceries", type = TransactionType.EXPENSE),
        )

        // Set: a fresh budget surfaces on the category's domain model.
        repository.setBudget(id, 200.0)
        assertEquals(200.0, repository.getCategoryStream(id).first()?.budget)

        // Update: upsert replaces the existing budget rather than adding a second row.
        repository.setBudget(id, 350.0)
        assertEquals(350.0, repository.getCategoryStream(id).first()?.budget)

        // Clear: a null amount removes the budget, leaving the category intact.
        repository.setBudget(id, null)
        val cleared = repository.getCategoryStream(id).first()
        assertEquals("Groceries", cleared?.name)
        assertNull(cleared?.budget)
    }

    @Test(expected = DuplicateCategoryException::class)
    fun updateCategory_duplicateName_throwsDuplicateCategoryException() = runBlocking {
        repository.insertCategory(Category(id = 0, name = "Groceries", type = TransactionType.EXPENSE))
        repository.insertCategory(Category(id = 0, name = "Salary", type = TransactionType.INCOME))

        val all = repository.getAllCategoriesStream().first()
        val salary = all.first { it.name == "Salary" }

        // Renaming "Salary" to an already-taken name is translated to DuplicateCategoryException.
        repository.updateCategory(salary.copy(name = "Groceries"))
        Unit
    }
}
