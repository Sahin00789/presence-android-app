package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "payment_rules")
data class PaymentRuleEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesId: String? = null,
    val ruleType: String = "", // HOLIDAY, WEEKLY_OFF, PAID_LEAVE
    val amountPaise: Long = 0L,
    val multiplier: Double = 1.0,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    val amountInr: Long get() = amountPaise
}

@Dao
interface PaymentRuleDao {
    @Query("SELECT * FROM payment_rules WHERE isDeleted = 0 ORDER BY ruleType ASC")
    fun observeAll(): Flow<List<PaymentRuleEntity>>

    @Query("SELECT * FROM payment_rules WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): PaymentRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PaymentRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PaymentRuleEntity>)

    @Query("DELETE FROM payment_rules")
    suspend fun clear()
}
