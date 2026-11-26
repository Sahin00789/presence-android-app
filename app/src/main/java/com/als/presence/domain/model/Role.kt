package com.als.presence.domain.model

/**
 * Core user roles in the Presence system.
 */
enum class Role {
    /** Organization Administrator with administrative and management access. */
    OWNER,

    /** Workforce employee assigned to a premises for attendance, shifts, and earnings. */
    EMPLOYEE
}
