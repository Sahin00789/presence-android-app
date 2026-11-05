package com.als.presence.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.als.presence.domain.model.Role
import com.als.presence.domain.model.User
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean = true,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val organizationName: String? = null,
    val organizationLogoUrl: String? = null,
    val organizationId: String? = null,
    val premisesId: String? = null,
    val premisesName: String? = null
) {
    fun toDomain(): User {
        val roleEnum = runCatching { Role.valueOf(role) }.getOrDefault(Role.EMPLOYEE)
        return User(
            id = id,
            email = email,
            fullName = fullName,
            role = roleEnum,
            isActive = isActive,
            avatarUrl = avatarUrl,
            phone = phone,
            organizationName = organizationName,
            organizationLogoUrl = organizationLogoUrl,
            organizationId = organizationId,
            premisesId = premisesId,
            premisesName = premisesName
        )
    }

    companion object {
        fun fromDomain(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role.name,
                isActive = user.isActive,
                avatarUrl = user.avatarUrl,
                phone = user.phone,
                organizationName = user.organizationName,
                organizationLogoUrl = user.organizationLogoUrl,
                organizationId = user.organizationId,
                premisesId = user.premisesId,
                premisesName = user.premisesName
            )
        }
    }
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}
