package io.github.asmahood.ledger.data.repository

import android.database.sqlite.SQLiteConstraintException
import io.github.asmahood.ledger.data.db.dao.CategoryDao
import io.github.asmahood.ledger.data.mapper.toEntity
import io.github.asmahood.ledger.data.mapper.toModel
import io.github.asmahood.ledger.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Thrown when a category cannot be saved because its name is already taken. Translates the
 * low-level Room/SQLite unique-constraint violation into a domain-level signal the UI can handle.
 */
class DuplicateCategoryException(message: String) : Exception(message)

interface CategoryRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>
    fun getCategoryStream(id: Long): Flow<Category?>
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

class OfflineCategoryRepository @Inject constructor(private val dao: CategoryDao) :
    CategoryRepository {
    override fun getAllCategoriesStream(): Flow<List<Category>> {
        return dao.getAllCategories().map { entities -> entities.map { it.toModel() } }
    }

    override fun getCategoryStream(id: Long): Flow<Category?> {
        return dao.getCategory(id).map { it?.toModel() }
    }

    override suspend fun insertCategory(category: Category) {
        try {
            dao.insert(category.toEntity())
        } catch (e: SQLiteConstraintException) {
            throw DuplicateCategoryException("A category named \"${category.name}\" already exists")
        }
    }

    override suspend fun updateCategory(category: Category) {
        try {
            return dao.update(category.toEntity())
        } catch (e: SQLiteConstraintException) {
            throw DuplicateCategoryException("A category named \"${category.name}\" already exists")
        }
    }

    override suspend fun deleteCategory(category: Category) {
        return dao.delete(category.toEntity())
    }
}
