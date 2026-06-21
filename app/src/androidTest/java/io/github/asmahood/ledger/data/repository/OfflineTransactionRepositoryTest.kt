package io.github.asmahood.ledger.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.asmahood.ledger.data.db.LedgerDatabase
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class OfflineTransactionRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var repository: OfflineTransactionRepository

    private lateinit var groceries: Category

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineTransactionRepository(db.transactionDao())

        // Transactions reference a category (foreign key), so seed one and capture its domain model.
        db.categoryDao().insert(
            CategoryEntity(name = "Groceries", description = "Food", type = TransactionType.EXPENSE.name),
        )
        val stored = db.categoryDao().getAllCategories().first().first()
        groceries = Category(
            id = stored.id,
            name = stored.name,
            type = TransactionType.valueOf(stored.type),
            description = stored.description,
        )
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
    ) = Transaction(
        id = 0,
        amount = amount,
        date = date,
        vendor = vendor,
        type = type,
        notes = notes,
        category = groceries,
    )

    @Test
    fun insertTransaction_persistsAndMapsBackToDomainModel() = runBlocking {
        repository.insertTransaction(
            transaction(amount = 12.50, vendor = "Corner Store", notes = "weekly shop"),
        )

        val all = repository.getAllTransactionsStream().first()
        assertEquals(1, all.size)
        val stored = all.first()
        assertEquals(12.50, stored.amount, 0.0)
        assertEquals("Corner Store", stored.vendor)
        assertEquals(LocalDate.of(2026, 6, 21), stored.date)
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals("weekly shop", stored.notes)
        // The embedded category round-trips through the join.
        assertEquals(groceries, stored.category)
    }

    @Test
    fun getAllTransactionsStream_orderedByDateDescending() = runBlocking {
        repository.insertTransaction(transaction(vendor = "Older", date = LocalDate.of(2026, 1, 1)))
        repository.insertTransaction(transaction(vendor = "Newest", date = LocalDate.of(2026, 6, 1)))
        repository.insertTransaction(transaction(vendor = "Middle", date = LocalDate.of(2026, 3, 1)))

        val vendors = repository.getAllTransactionsStream().first().map { it.vendor }
        assertEquals(listOf("Newest", "Middle", "Older"), vendors)
    }

    @Test
    fun getTransactionStream_returnsMatchingTransaction() = runBlocking {
        repository.insertTransaction(transaction(vendor = "Corner Store"))
        val id = repository.getAllTransactionsStream().first().first().id

        val fetched = repository.getTransactionStream(id).first()
        assertEquals("Corner Store", fetched?.vendor)
        assertEquals(groceries, fetched?.category)
    }

    @Test
    fun getTransactionStream_missingId_returnsNull() = runBlocking {
        val fetched = repository.getTransactionStream(999L).first()

        assertNull(fetched)
    }

    @Test
    fun updateTransaction_persistsChanges() = runBlocking {
        repository.insertTransaction(transaction(amount = 10.0, vendor = "Corner Store"))
        val stored = repository.getAllTransactionsStream().first().first()

        repository.updateTransaction(stored.copy(amount = 42.0, vendor = "Updated"))

        val updated = repository.getTransactionStream(stored.id).first()
        assertEquals(42.0, updated?.amount)
        assertEquals("Updated", updated?.vendor)
    }

    @Test
    fun deleteTransaction_removesTransaction() = runBlocking {
        repository.insertTransaction(transaction())
        val stored = repository.getAllTransactionsStream().first().first()

        repository.deleteTransaction(stored)

        assertEquals(0, repository.getAllTransactionsStream().first().size)
    }
}
