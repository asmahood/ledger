package io.github.asmahood.ledger.data.db.dao

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.data.db.LedgerDatabase
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.entity.TransactionEntity
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    private var groceriesId: Long = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
        categoryDao = db.categoryDao()

        // Transactions require an existing category (foreign key), so seed one first.
        categoryDao.insert(
            CategoryEntity(name = "Groceries", description = "Food", type = TransactionType.EXPENSE.name),
        )
        groceriesId = categoryDao.getAllCategories().first().first().category.id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    private fun transaction(
        amount: Double = 12.50,
        date: LocalDate = LocalDate.of(2026, 6, 21),
        vendor: String = "Corner Store",
        type: TransactionType = TransactionType.EXPENSE,
        notes: String? = null,
        categoryId: Long = groceriesId,
    ) = TransactionEntity(
        amount = amount,
        date = date,
        vendor = vendor,
        type = type.name,
        notes = notes,
        categoryId = categoryId,
    )

    @Test
    fun transactionDao_insert_persistsAndIsReadable() = runBlocking {
        dao.insert(transaction(vendor = "Corner Store"))

        val all = dao.getAllTransactions().first()
        assertEquals(1, all.size)
        assertEquals("Corner Store", all.first().transaction.vendor)
    }

    @Test
    fun transactionDao_insert_autoGeneratesId() = runBlocking {
        dao.insert(transaction())

        val inserted = dao.getAllTransactions().first().first().transaction
        assertTrue("Expected a non-zero generated id", inserted.id > 0)
    }

    @Test
    fun transactionDao_insert_joinsCategory() = runBlocking {
        dao.insert(transaction())

        val withCategory = dao.getAllTransactions().first().first()
        assertEquals(groceriesId, withCategory.category.id)
        assertEquals("Groceries", withCategory.category.name)
    }

    @Test
    fun transactionDao_getAllTransactions_orderedByDateDesc() = runBlocking {
        dao.insert(transaction(vendor = "Older", date = LocalDate.of(2026, 1, 1)))
        dao.insert(transaction(vendor = "Newest", date = LocalDate.of(2026, 6, 1)))
        dao.insert(transaction(vendor = "Middle", date = LocalDate.of(2026, 3, 1)))

        val vendors = dao.getAllTransactions().first().map { it.transaction.vendor }
        assertEquals(listOf("Newest", "Middle", "Older"), vendors)
    }

    @Test
    fun transactionDao_getTransaction_returnsMatchingRowWithCategory() = runBlocking {
        dao.insert(transaction(vendor = "Corner Store", notes = "weekly shop"))
        val id = dao.getAllTransactions().first().first().transaction.id

        val fetched = dao.getTransaction(id).first()
        assertEquals("Corner Store", fetched?.transaction?.vendor)
        assertEquals("weekly shop", fetched?.transaction?.notes)
        assertEquals("Groceries", fetched?.category?.name)
    }

    @Test
    fun transactionDao_getTransaction_missingId_returnsNull() = runBlocking {
        val fetched = dao.getTransaction(999L).first()

        assertNull(fetched)
    }

    @Test
    fun transactionDao_update_modifiesRow() = runBlocking {
        dao.insert(transaction(amount = 10.0))
        val inserted = dao.getAllTransactions().first().first().transaction

        dao.update(inserted.copy(amount = 42.0, vendor = "Updated"))

        val updated = dao.getTransaction(inserted.id).first()?.transaction
        assertEquals(42.0, updated?.amount)
        assertEquals("Updated", updated?.vendor)
    }

    @Test
    fun transactionDao_delete_removesRow() = runBlocking {
        dao.insert(transaction())
        val inserted = dao.getAllTransactions().first().first().transaction

        dao.delete(inserted)

        assertTrue(dao.getAllTransactions().first().isEmpty())
    }

    @Test
    fun transactionDao_roundTripsLocalDate() = runBlocking {
        val date = LocalDate.of(2026, 2, 14)
        dao.insert(transaction(date = date))

        val stored = dao.getAllTransactions().first().first().transaction
        assertEquals(date, stored.date)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun transactionDao_insertWithUnknownCategory_throwsConstraintException() = runBlocking {
        // No category with this id exists, so the foreign key constraint is violated.
        dao.insert(transaction(categoryId = 9999L))
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun transactionDao_deletingReferencedCategory_throwsConstraintException() = runBlocking {
        dao.insert(transaction())
        val category = categoryDao.getAllCategories().first().first().category

        // The transaction's category has onDelete = RESTRICT, so this must fail.
        categoryDao.delete(category)
        Unit
    }
}
