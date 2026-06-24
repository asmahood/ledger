package io.github.asmahood.ledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.relation.CategoryWithBudget
import io.github.asmahood.ledger.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete()
    suspend fun delete(category: CategoryEntity)

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategory(id: Long): Flow<CategoryWithBudget?>

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryWithBudget>>
}