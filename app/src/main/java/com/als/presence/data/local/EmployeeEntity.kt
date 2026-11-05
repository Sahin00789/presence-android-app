package com.als.presence.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.als.presence.data.model.StaffDto

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val organizationName: String? = null,
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val staffCode: String = "",
    val department: String? = null,
    val designation: String? = null,
    val role: String = "STAFF",
    val token: String? = null,
    val addedBy: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null,
    val hireDate: String? = null,
    val joinedAt: String? = null,
    val createdAt: String? = null,
    val isActive: Boolean = true,
    val earnedBalancePaise: Long = 0L,
    val approvedUnpaidBalancePaise: Long = 0L,
    val paidBalancePaise: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    val earnedBalanceInr: Long get() = earnedBalancePaise
    val approvedUnpaidBalanceInr: Long get() = approvedUnpaidBalancePaise
    val paidBalanceInr: Long get() = paidBalancePaise

    fun toDto(): StaffDto {
        return StaffDto(
            id = id,
            organizationId = organizationId,
            organizationName = organizationName,
            userId = userId,
            email = email,
            fullName = fullName,
            staffCode = staffCode,
            department = department,
            designation = designation,
            role = role,
            token = token,
            addedBy = addedBy,
            premisesId = premisesId,
            premisesName = premisesName,
            hireDate = hireDate,
            joinedAt = joinedAt,
            createdAt = createdAt,
            isActive = isActive,
            earnedBalancePaise = earnedBalancePaise,
            approvedUnpaidBalancePaise = approvedUnpaidBalancePaise,
            paidBalancePaise = paidBalancePaise
        )
    }

    companion object {
        fun fromDto(dto: StaffDto, version: Long = 1L): EmployeeEntity {
            return EmployeeEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                organizationName = dto.organizationName,
                userId = dto.userId,
                email = dto.email,
                fullName = dto.fullName,
                staffCode = dto.staffCode,
                department = dto.department,
                designation = dto.designation,
                role = dto.role,
                token = dto.token,
                addedBy = dto.addedBy,
                premisesId = dto.premisesId,
                premisesName = dto.premisesName,
                hireDate = dto.hireDate,
                joinedAt = dto.joinedAt,
                createdAt = dto.createdAt,
                isActive = dto.isActive,
                earnedBalancePaise = dto.earnedBalancePaise,
                approvedUnpaidBalancePaise = dto.approvedUnpaidBalancePaise,
                paidBalancePaise = dto.paidBalancePaise,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}
