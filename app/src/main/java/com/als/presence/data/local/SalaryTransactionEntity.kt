package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.PaymentRecordDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "salary_transactions")
data class SalaryTransactionEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val monthlyPayrollId: String = "",
    val staffProfileId: String = "",
    val amountPaise: Long = 0L,
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val referenceNumber: String? = null,
    val recordedBy: String? = null,
    val createdAt: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    val amountInr: Long get() = amountPaise

    fun toDto(): PaymentRecordDto {
        return PaymentRecordDto(
            id = id,
            amountPaise = amountPaise,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            referenceNumber = referenceNumber,
            recordedBy = recordedBy
        )
    }

    companion object {
        fun fromDto(dto: PaymentRecordDto, monthlyPayrollId: String = "", staffProfileId: String = ""): SalaryTransactionEntity {
            return SalaryTransactionEntity(
                id = dto.id.ifBlank { java.util.UUID.randomUUID().toString() },
                monthlyPayrollId = monthlyPayrollId,
                staffProfileId = staffProfileId,
                amountPaise = dto.amountPaise,
                paymentDate = dto.paymentDate,
                paymentMethod = dto.paymentMethod,
                referenceNumber = dto.referenceNumber,
                recordedBy = dto.recordedBy,
                createdAt = dto.paymentDate,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

@Dao
interface SalaryTransactionDao {
    @Query("SELECT * FROM salary_transactions WHERE staffProfileId = :staffProfileId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun observeByStaff(staffProfileId: String): Flow<List<SalaryTransactionEntity>>

    @Query("SELECT * FROM salary_transactions WHERE staffProfileId = :staffProfileId AND isDeleted = 0 ORDER BY paymentDate DESC")
    suspend fun getByStaff(staffProfileId: String): List<SalaryTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SalaryTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SalaryTransactionEntity>)

    @Query("DELETE FROM salary_transactions")
    suspend fun clear()
}
