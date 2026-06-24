package io.github.asmahood.ledger.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.data.db.LedgerDatabase
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
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
class BudgetDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: BudgetDao
    private lateinit var categoryDao: CategoryDao

    private var groceriesId: Long = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.budgetDao()
        categoryDao = db.categoryDao()

        // Budgets are keyed by an existing category (foreign key), so seed one first.
        groceriesId = categoryDao.insert(
            CategoryEntity(name = "Groceries", description = "Food", type = TransactionType.EXPENSE.name),
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun budgetDao_upsert_insertsAndIsReadable() = runBlocking {
        dao.upsert(BudgetEntity(categoryId = groceriesId, monthlyAmount = 200.0))

        val stored = dao.getByCategory(groceriesId).first()
        assertEquals(200.0, stored?.monthlyAmount)
    }

    @Test
    fun budgetDao_upsert_replacesExistingRow() = runBlocking {
        dao.upsert(BudgetEntity(categoryId = groceriesId, monthlyAmount = 200.0))
        dao.upsert(BudgetEntity(categoryId = groceriesId, monthlyAmount = 350.0))

        // The primary key is category_id, so the second upsert updates rather than adds a row.
        val stored = dao.getByCategory(groceriesId).first()
        assertEquals(350.0, stored?.monthlyAmount)
    }

    @Test
    fun budgetDao_getByCategory_missing_returnsNull() = runBlocking {
        assertNull(dao.getByCategory(groceriesId).first())
    }

    @Test
    fun budgetDao_deleteByCategory_removesRow() = runBlocking {
        dao.upsert(BudgetEntity(categoryId = groceriesId, monthlyAmount = 200.0))

        dao.deleteByCategory(groceriesId)

        assertNull(dao.getByCategory(groceriesId).first())
    }

    @Test
    fun budgetDao_deletingOwningCategory_cascadeDeletesBudget() = runBlocking {
        dao.upsert(BudgetEntity(categoryId = groceriesId, monthlyAmount = 200.0))
        val category = categoryDao.getCategory(groceriesId).first()!!.category

        // The budget's FK uses onDelete = CASCADE, so removing the category drops its budget.
        categoryDao.delete(category)

        assertNull(dao.getByCategory(groceriesId).first())
    }
}
