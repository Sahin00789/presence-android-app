package com.als.presence.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class PostOffice(
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String? = null,
    @SerialName("BranchType") val branchType: String = "",
    @SerialName("DeliveryStatus") val deliveryStatus: String = "",
    @SerialName("Circle") val circle: String = "",
    @SerialName("District") val district: String = "",
    @SerialName("Division") val division: String = "",
    @SerialName("Region") val region: String = "",
    @SerialName("State") val state: String = "",
    @SerialName("Country") val country: String = "",
    @SerialName("Pincode") val pincode: String = "",
    @SerialName("Block") val block: String? = null,
    @SerialName("Taluk") val taluk: String? = null
) {
    val subDistrict: String? get() = taluk
}

sealed class DomainWebSocketEvent(
    open val isOwnEvent: Boolean = false,
    open val toastMessage: String? = null
) {
    data class Event(
        val type: String = "",
        val payload: String? = null,
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = null
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class PremisesUpdate(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Premises updated"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class AttendanceUpdate(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Attendance updated"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class StaffUpdate(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Staff details updated"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class ShiftUpdate(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Shift roster updated"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class WalletUpdate(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Salary & ledger updated"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)

    data class Generic(
        override val isOwnEvent: Boolean = false,
        override val toastMessage: String? = "Real-time update received"
    ) : DomainWebSocketEvent(isOwnEvent, toastMessage)
}
