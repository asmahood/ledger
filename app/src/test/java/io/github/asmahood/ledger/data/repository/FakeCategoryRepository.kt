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
 * - Inspect writes via [inserted] / [updated] / [deleted].
 */
class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    val inserted = mutableListOf<Category>()
    val updated = mutableListOf<Category>()
    val deleted = mutableListOf<Category>()

    var streamError: Throwable? = null
    var insertError: Throwable? = null

    fun setCategories(values: List<Category>) {
        categories.value = values
    }

    override fun getAllCategoriesStream(): Flow<List<Category>> =
        streamError?.let { error -> flow<List<Category>> { throw error } } ?: categories

    override fun getCategoryStream(id: Long): Flow<Category?> =
        categories.map { list -> list.find { it.id == id } }

    override suspend fun insertCategory(category: Category) {
        insertError?.let { throw it }
        inserted += category
        categories.value = categories.value + category
    }

    override suspend fun updateCategory(category: Category) {
        updated += category
    }

    override suspend fun deleteCategory(category: Category) {
        deleted += category
    }
}
