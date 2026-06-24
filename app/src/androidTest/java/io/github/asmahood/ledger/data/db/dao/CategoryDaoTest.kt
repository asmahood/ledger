package io.github.asmahood.ledger.data.db.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: CategoryDao
    private lateinit var budgetDao: BudgetDao

    private val groceries = CategoryEntity(
        name = "Groceries",
        description = "Food and household",
        type = TransactionType.EXPENSE.name,
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.categoryDao()
        budgetDao = db.budgetDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun categoryDao_insert_persistsAndIsReadable() = runBlocking {
        dao.insert(groceries)

        val all = dao.getAllCategories().first()
        assertEquals(1, all.size)
        assertEquals("Groceries", all.first().category.name)
    }

    @Test
    fun categoryDao_insert_autoGeneratesId() = runBlocking {
        dao.insert(groceries)

        val inserted = dao.getAllCategories().first().first().category
        assertTrue("Expected a non-zero generated id", inserted.id > 0)
    }

    @Test
    fun categoryDao_getAllCategories_orderedByNameAsc() = runBlocking {
        dao.insert(CategoryEntity(name = "Rent", description = null, type = TransactionType.EXPENSE.name))
        dao.insert(CategoryEntity(name = "Auto", description = null, type = TransactionType.EXPENSE.name))
        dao.insert(CategoryEntity(name = "Groceries", description = null, type = TransactionType.EXPENSE.name))

        val names = dao.getAllCategories().first().map { it.category.name }
        assertEquals(listOf("Auto", "Groceries", "Rent"), names)
    }

    @Test
    fun categoryDao_getCategory_returnsMatchingRow() = runBlocking {
        dao.insert(groceries)
        val id = dao.getAllCategories().first().first().category.id

        val fetched = dao.getCategory(id).first()
        assertEquals("Groceries", fetched?.category?.name)
    }

    @Test
    fun categoryDao_getCategory_missingId_returnsNull() = runBlocking {
        val fetched = dao.getCategory(999L).first()

        assertNull(fetched)
    }

    @Test
    fun categoryDao_update_modifiesRow() = runBlocking {
        dao.insert(groceries)
        val inserted = dao.getAllCategories().first().first().category

        dao.update(inserted.copy(name = "Food"))

        val updated = dao.getCategory(inserted.id).first()
        assertEquals("Food", updated?.category?.name)
    }

    @Test
    fun categoryDao_delete_removesRow() = runBlocking {
        dao.insert(groceries)
        val inserted = dao.getAllCategories().first().first().category

        dao.delete(inserted)

        assertTrue(dao.getAllCategories().first().isEmpty())
    }

    @Test
    fun categoryDao_getCategory_noBudget_relationIsNull() = runBlocking {
        dao.insert(groceries)
        val id = dao.getAllCategories().first().first().category.id

        val fetched = dao.getCategory(id).first()
        assertNull(fetched?.budget)
    }

    @Test
    fun categoryDao_getCategory_joinsBudgetThroughRelation() = runBlocking {
        dao.insert(groceries)
        val id = dao.getAllCategories().first().first().category.id
        budgetDao.upsert(BudgetEntity(categoryId = id, monthlyAmount = 250.0))

        val fetched = dao.getCategory(id).first()
        assertEquals(250.0, fetched?.budget?.monthlyAmount)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun categoryDao_insertDuplicateName_throwsConstraintException() = runBlocking {
        dao.insert(CategoryEntity(name = "Groceries", description = null, type = TransactionType.EXPENSE.name))
        // Same name, even with a different type, violates the unique index on `name`.
        dao.insert(CategoryEntity(name = "Groceries", description = null, type = TransactionType.INCOME.name))
        Unit
    }
}
