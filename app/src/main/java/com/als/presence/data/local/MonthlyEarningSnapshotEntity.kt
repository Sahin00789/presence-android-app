package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.MonthlyEarningLedgerResponseDto

@Entity(tableName = "monthly_earning_snapshots")
data class MonthlyEarningSnapshotEntity(
    @PrimaryKey val id: String,
    val month: Int,
    val year: Int,
    val staffProfileId: String,
    val staffName: String = "",
    val staffCode: String = "",
    val premisesId: String? = null,
    val earnedAmount: String = "0.00",
    val earnedPaise: Long = 0L,
    val deductionAmount: String = "0.00",
    val paidAmount: String = "0.00",
    val dueAmount: String = "0.00",
    val status: String = "OPEN",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val earnedInr: Long get() = earnedPaise

    fun toDto(): MonthlyEarningLedgerResponseDto {
        return MonthlyEarningLedgerResponseDto(
            staffProfileId = staffProfileId,
            staffName = staffName,
            staffCode = staffCode,
            premisesId = premisesId,
            month = month,
            year = year,
            earnedAmount = earnedAmount,
            earnedPaise = earnedPaise,
            deductionAmount = deductionAmount,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
            status = status
        )
    }

    companion object {
        fun fromDto(dto: MonthlyEarningLedgerResponseDto): MonthlyEarningSnapshotEntity {
            val key = "${dto.month}-${dto.year}-${dto.staffProfileId}"
            return MonthlyEarningSnapshotEntity(
                id = key,
                month = dto.month,
                year = dto.year,
                staffProfileId = dto.staffProfileId,
                staffName = dto.staffName,
                staffCode = dto.staffCode,
                premisesId = dto.premisesId,
                earnedAmount = dto.earnedAmount,
                earnedPaise = dto.earnedPaise,
                deductionAmount = dto.deductionAmount,
                paidAmount = dto.paidAmount,
                dueAmount = dto.dueAmount,
                status = dto.status
            )
        }
    }
}
