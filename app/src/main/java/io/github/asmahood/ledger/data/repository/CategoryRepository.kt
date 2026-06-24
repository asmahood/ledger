package io.github.asmahood.ledger.data.repository

import android.database.sqlite.SQLiteConstraintException
import io.github.asmahood.ledger.data.db.dao.BudgetDao
import io.github.asmahood.ledger.data.db.dao.CategoryDao
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import io.github.asmahood.ledger.data.db.relation.CategoryWithBudget
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
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun setBudget(categoryId: Long, amount: Double?)
}

class OfflineCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
) :
    CategoryRepository {
    override fun getAllCategoriesStream(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities -> entities.map { it.toModel() } }
    }

    override fun getCategoryStream(id: Long): Flow<Category?> {
        return categoryDao.getCategory(id).map { it?.toModel() }
    }

    override suspend fun insertCategory(category: Category): Long {
        try {
            return categoryDao.insert(category.toEntity())
        } catch (e: SQLiteConstraintException) {
            throw DuplicateCategoryException("A category named \"${category.name}\" already exists")
        }
    }

    override suspend fun updateCategory(category: Category) {
        try {
            return categoryDao.update(category.toEntity())
        } catch (e: SQLiteConstraintException) {
            throw DuplicateCategoryException("A category named \"${category.name}\" already exists")
        }
    }

    override suspend fun deleteCategory(category: Category) {
        return categoryDao.delete(category.toEntity())
    }

    override suspend fun setBudget(categoryId: Long, amount: Double?) {
        if (amount != null) {
            return budgetDao.upsert(BudgetEntity(
                categoryId = categoryId,
                monthlyAmount = amount
            ))
        }

        return budgetDao.deleteByCategory(categoryId)
    }
}
