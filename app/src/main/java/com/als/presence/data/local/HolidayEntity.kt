package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.data.model.HolidayDto
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val id: String,
    val organizationId: String = "",
    val premisesId: String? = null,
    val holidayDate: String = "",
    val name: String = "",
    val description: String? = null,
    val isPaid: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isDeleted: Boolean = false
) {
    fun toDto(): HolidayDto {
        return HolidayDto(
            id = id,
            organizationId = organizationId,
            premisesId = premisesId,
            holidayDate = holidayDate,
            name = name,
            description = description,
            isPaid = isPaid,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDto(dto: HolidayDto, version: Long = 1L): HolidayEntity {
            return HolidayEntity(
                id = dto.id,
                organizationId = dto.organizationId,
                premisesId = dto.premisesId,
                holidayDate = dto.holidayDate,
                name = dto.name,
                description = dto.description,
                isPaid = dto.isPaid,
                createdAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
                version = version,
                isDeleted = false
            )
        }
    }
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays WHERE isDeleted = 0 ORDER BY holidayDate ASC")
    fun observeAll(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE isDeleted = 0 ORDER BY holidayDate ASC")
    suspend fun getAll(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE (premisesId = :premisesId OR premisesId IS NULL) AND isDeleted = 0 ORDER BY holidayDate ASC")
    fun observeByPremises(premisesId: String): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getById(id: String): HolidayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HolidayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<HolidayEntity>)

    @Query("UPDATE holidays SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM holidays")
    suspend fun clear()
}
