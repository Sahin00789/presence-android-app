package com.als.presence.domain.model

enum class Role {
    OWNER,
    MANAGER,
    STAFF;

    companion object {
        fun fromString(value: String): Role {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: STAFF
        }
    }
}
