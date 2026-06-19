package io.github.asmahood.ledger.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The name of this category
     */
    val name: String,

    /**
     * An optional description of what types of transactions this category is used for
     */
    val description: String?,

    /**
     * The type of transaction. Either "EXPENSE" or "INCOME"
     */
    val type: String,
)
