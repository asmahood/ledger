package io.github.asmahood.ledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The total transaction amount
     */
    val amount: Double,

    /**
     * The date the transaction occurred as an EPOCH timestamp
     */
    val date: Long,

    /**
     * The vendor/source of the transaction
     */
    val vendor: String,

    /**
     * The type of transaction (either EXPENSE or INCOME)
     */
    val type: String,

    /**
     * Additional context with the transaction
     */
    val notes: String?,

    /**
     * The category of this transaction
     */
    @ColumnInfo(name = "category_id")
    val categoryId: Long
)
