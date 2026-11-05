package com.als.presence.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OlaMapsAddressComponent(
    @SerialName("long_name") val longName: String = "",
    @SerialName("short_name") val shortName: String = "",
    @SerialName("types") val types: List<String> = emptyList()
)

@Serializable
data class OlaMapsGeocodeResult(
    @SerialName("formatted_address") val formattedAddress: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("address_components") val addressComponents: List<OlaMapsAddressComponent> = emptyList()
)

@Serializable
data class OlaMapsReverseGeocodeResponse(
    @SerialName("status") val status: String = "",
    @SerialName("results") val results: List<OlaMapsGeocodeResult> = emptyList(),
    @SerialName("result") val result: OlaMapsGeocodeResult? = null
)
