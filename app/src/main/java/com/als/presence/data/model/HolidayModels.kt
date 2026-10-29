package com.als.presence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HolidayDto(
    val id: String = "",
    val organizationId: String = "",
    val premisesId: String? = null,
    val holidayDate: String = "",
    val name: String = "",
    val description: String? = null,
    val isPaid: Boolean = true,
    val createdAt: String? = null
) {
    val date: String get() = holidayDate
}

@Serializable
data class CreateHolidayRequest(
    val premisesId: String? = null,
    val holidayDate: String = "",
    val name: String = "",
    val description: String? = null,
    val isPaid: Boolean = true
) {
    val date: String get() = holidayDate
}

@Serializable
data class UpdateHolidayRequest(
    val holidayDate: String? = null,
    val name: String? = null,
    val description: String? = null,
    val isPaid: Boolean? = null
) {
    val date: String? get() = holidayDate
}
