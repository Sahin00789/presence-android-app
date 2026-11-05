package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owner_employees_snapshots")
data class OwnerEmployeesSnapshotEntity(
    @PrimaryKey val premisesId: String, // premisesId or "ALL"
    val totalEmployees: Int = 0,
    val activeEmployees: Int = 0,
    val employeesJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface OwnerEmployeesSnapshotDao {
    @Query("SELECT * FROM owner_employees_snapshots WHERE premisesId = :premisesId LIMIT 1")
    fun observeByPremises(premisesId: String): Flow<OwnerEmployeesSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OwnerEmployeesSnapshotEntity)

    @Query("DELETE FROM owner_employees_snapshots")
    suspend fun clear()
}
