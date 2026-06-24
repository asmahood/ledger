package io.github.asmahood.ledger.data.repository

import io.github.asmahood.ledger.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Controllable fake for [CategoryRepository] used in ViewModel unit tests.
 *
 * - Drive the categories stream with [setCategories].
 * - Make the stream fail by setting [streamError] (covers ViewModel error paths).
 * - Inspect writes via [inserted] / [updated] / [deleted] / [budgets].
 */
class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    val inserted = mutableListOf<Category>()
    val updated = mutableListOf<Category>()
    val deleted = mutableListOf<Category>()

    /** In-memory budget storage keyed by category id; mirrors what [setBudget] persists. */
    val budgets = mutableMapOf<Long, Double>()

    private var nextId = 1L

    var streamError: Throwable? = null
    var insertError: Throwable? = null
    var updateError: Throwable? = null
    var deleteError: Throwable? = null

    fun setCategories(values: List<Category>) {
        categories.value = values
    }

    override fun getAllCategoriesStream(): Flow<List<Category>> =
        streamError?.let { error -> flow<List<Category>> { throw error } } ?: categories

    override fun getCategoryStream(id: Long): Flow<Category?> =
        categories.map { list -> list.find { it.id == id } }

    override suspend fun insertCategory(category: Category): Long {
        insertError?.let { throw it }
        inserted += category
        categories.value = categories.value + category
        return nextId++
    }

    override suspend fun updateCategory(category: Category) {
        updateError?.let { throw it }
        updated += category
        categories.value = categories.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun deleteCategory(category: Category) {
        deleteError?.let { throw it }
        deleted += category
    }

    override suspend fun setBudget(categoryId: Long, amount: Double?) {
        if (amount != null) {
            budgets[categoryId] = amount
        } else {
            budgets.remove(categoryId)
        }
        categories.value = categories.value.map {
            if (it.id == categoryId) it.copy(budget = amount) else it
        }
    }
}
