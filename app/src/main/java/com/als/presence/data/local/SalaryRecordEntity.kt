package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.MonthlyEarningLedgerResponseDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "salary_records")
data class SalaryRecordEntity(
    @PrimaryKey val id: String, // format "$month-$year-$staffProfileId"
    val organizationId: String = "",
    val staffProfileId: String = "",
    val staffName: String = "",
    val staffCode: String = "",
    val premisesId: String? = null,
    val month: Int = 1,
    val year: Int = 2025,
    val earnedAmountPaise: Long = 0L,
    val deductionAmountPaise: Long = 0L,
    val paidAmountPaise: Long = 0L,
    val dueAmountPaise: Long = 0L,
    val status: String = "OPEN",
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    val earnedAmountInr: Long get() = earnedAmountPaise
    val deductionAmountInr: Long get() = deductionAmountPaise
    val paidAmountInr: Long get() = paidAmountPaise
    val dueAmountInr: Long get() = dueAmountPaise

    fun toDto(): MonthlyEarningLedgerResponseDto {
        return MonthlyEarningLedgerResponseDto(
            staffProfileId = staffProfileId,
            staffName = staffName,
            staffCode = staffCode,
            premisesId = premisesId,
            month = month,
            year = year,
            earnedPaise = earnedAmountPaise,
            status = status
        )
    }

    companion object {
        fun fromDto(dto: MonthlyEarningLedgerResponseDto, version: Long = 1L): SalaryRecordEntity {
            val key = "${dto.month}-${dto.year}-${dto.staffProfileId}"
            return SalaryRecordEntity(
                id = key,
                staffProfileId = dto.staffProfileId,
                staffName = dto.staffName,
                staffCode = dto.staffCode,
                premisesId = dto.premisesId,
                month = dto.month,
                year = dto.year,
                earnedAmountPaise = dto.earnedPaise,
                status = dto.status,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}

@Dao
interface SalaryRecordDao {
    @Query("SELECT * FROM salary_records WHERE staffProfileId = :staffId AND month = :month AND year = :year AND isDeleted = 0 LIMIT 1")
    fun observeForStaffMonth(staffId: String, month: Int, year: Int): Flow<SalaryRecordEntity?>

    @Query("SELECT * FROM salary_records WHERE month = :month AND year = :year AND isDeleted = 0")
    fun observeForMonth(month: Int, year: Int): Flow<List<SalaryRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SalaryRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SalaryRecordEntity>)

    @Query("DELETE FROM salary_records")
    suspend fun clear()
}
