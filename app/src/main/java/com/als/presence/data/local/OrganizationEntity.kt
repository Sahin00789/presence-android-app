package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.OrganizationDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String = "",
    val organizationName: String = "",
    val timezone: String = "UTC",
    val currency: String = "INR",
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): OrganizationDto {
        return OrganizationDto(
            id = id,
            ownerUserId = ownerUserId,
            organizationName = organizationName,
            timezone = timezone,
            currency = currency,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: OrganizationDto, version: Long = 1L): OrganizationEntity {
            return OrganizationEntity(
                id = dto.id,
                ownerUserId = dto.ownerUserId,
                organizationName = dto.organizationName,
                timezone = dto.timezone,
                currency = dto.currency,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organizations WHERE isDeleted = 0 LIMIT 1")
    fun observeCurrentOrganization(): Flow<OrganizationEntity?>

    @Query("SELECT * FROM organizations WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OrganizationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OrganizationEntity>)

    @Query("UPDATE organizations SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM organizations")
    suspend fun clear()
}
