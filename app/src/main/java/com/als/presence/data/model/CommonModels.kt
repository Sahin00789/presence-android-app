package com.als.presence.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class PostalResponseDto(
    @SerialName("Message") val message: String? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("PostOffice") val postOffice: List<PostOffice>? = null
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

sealed class DomainWebSocketEvent {
    data class Event(val type: String = "", val payload: String? = null) : DomainWebSocketEvent()
    object PremisesUpdate : DomainWebSocketEvent()
    object AttendanceUpdate : DomainWebSocketEvent()
    object StaffUpdate : DomainWebSocketEvent()
    object ShiftUpdate : DomainWebSocketEvent()
    object WalletUpdate : DomainWebSocketEvent()
    object Generic : DomainWebSocketEvent()
}
