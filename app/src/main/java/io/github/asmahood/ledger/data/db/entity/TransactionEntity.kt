package io.github.asmahood.ledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("category_id")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The total transaction amount
     */
    val amount: Double,

    /**
     * The date the transaction occurred
     */
    val date: LocalDate,

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
