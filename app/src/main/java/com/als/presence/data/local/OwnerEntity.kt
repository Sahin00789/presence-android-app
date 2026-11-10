package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owners")
data class OwnerEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val organizationId: String = "",
    val organizationName: String = "",
    val organizationLogoUrl: String? = null,
    val currency: String = "INR",
    val timezone: String = "Asia/Kolkata",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface OwnerDao {
    @Query("SELECT * FROM owners WHERE id = :ownerId LIMIT 1")
    suspend fun getById(ownerId: String): OwnerEntity?

    @Query("SELECT * FROM owners LIMIT 1")
    fun observeCurrentOwner(): Flow<OwnerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(owner: OwnerEntity)

    @Query("DELETE FROM owners")
    suspend fun clear()
}
