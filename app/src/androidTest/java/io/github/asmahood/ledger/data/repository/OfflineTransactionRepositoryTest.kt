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
        val stored = db.categoryDao().getAllCategories().first().first().category
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

    @Test
    fun getPeriodTotalsStream_sumsIncomeAndExpensesWithinRange() = runBlocking {
        repository.insertTransaction(
            transaction(amount = 4000.0, date = LocalDate.of(2026, 6, 10), type = TransactionType.INCOME),
        )
        repository.insertTransaction(
            transaction(amount = 1500.0, date = LocalDate.of(2026, 6, 12), type = TransactionType.EXPENSE),
        )
        // Outside the queried month, so it must not be counted.
        repository.insertTransaction(
            transaction(amount = 999.0, date = LocalDate.of(2026, 7, 1), type = TransactionType.EXPENSE),
        )

        val totals = repository.getPeriodTotalsStream(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
        ).first()
        assertEquals(4000.0, totals.income!!, 0.001)
        assertEquals(1500.0, totals.expenses!!, 0.001)
    }

    @Test
    fun getPeriodTotalsStream_noTransactionsInRange_returnsNulls() = runBlocking {
        repository.insertTransaction(transaction(date = LocalDate.of(2026, 1, 1)))

        val totals = repository.getPeriodTotalsStream(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
        ).first()
        assertNull(totals.income)
        assertNull(totals.expenses)
    }

    @Test
    fun getMonthlyTotalsByTypeStream_sumsIncomePerMonthWithinRange() = runBlocking {
        repository.insertTransaction(
            transaction(amount = 4000.0, date = LocalDate.of(2026, 1, 10), type = TransactionType.INCOME),
        )
        repository.insertTransaction(
            transaction(amount = 200.0, date = LocalDate.of(2026, 1, 20), type = TransactionType.INCOME),
        )
        repository.insertTransaction(
            transaction(amount = 3950.0, date = LocalDate.of(2026, 2, 10), type = TransactionType.INCOME),
        )
        // Not income, and outside the range respectively; neither should be counted.
        repository.insertTransaction(
            transaction(amount = 100.0, date = LocalDate.of(2026, 1, 10), type = TransactionType.EXPENSE),
        )
        repository.insertTransaction(
            transaction(amount = 999.0, date = LocalDate.of(2026, 4, 1), type = TransactionType.INCOME),
        )

        val rows = repository.getMonthlyTotalsByTypeStream(
            TransactionType.INCOME, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
        ).first()

        assertEquals(
            listOf(1 to 4200.0, 2 to 3950.0),
            rows.map { it.month to it.total },
        )
    }

    @Test
    fun getMonthlyTotalsByTypeStream_noMatchingTransactions_returnsEmpty() = runBlocking {
        repository.insertTransaction(
            transaction(amount = 100.0, date = LocalDate.of(2026, 1, 10), type = TransactionType.EXPENSE),
        )

        val rows = repository.getMonthlyTotalsByTypeStream(
            TransactionType.INCOME, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
        ).first()

        assertEquals(0, rows.size)
    }

    @Test
    fun insertTransactions_persistsEveryRowInTheBatch() = runBlocking {
        repository.insertTransactions(
            listOf(
                transaction(amount = 12.50, vendor = "Food Basics"),
                transaction(amount = 4.25, vendor = "Blue Bottle"),
            ),
        )

        val stored = repository.getAllTransactionsStream().first()

        assertEquals(2, stored.size)
        assertEquals(setOf("Food Basics", "Blue Bottle"), stored.map { it.vendor }.toSet())
        assertEquals(groceries, stored.first().category)
    }

    @Test
    fun insertTransactions_emptyList_writesNothing() = runBlocking {
        repository.insertTransactions(emptyList())

        assertEquals(0, repository.getAllTransactionsStream().first().size)
    }
}
