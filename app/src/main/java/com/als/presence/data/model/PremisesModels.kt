package com.als.presence.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Serializer that parses Double numbers or String representation of Double numbers from JSON.
 */
object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.doubleOrNull ?: element.content.toDoubleOrNull() ?: 0.0
            } else {
                0.0
            }
        } else {
            try {
                decoder.decodeDouble()
            } catch (_: Exception) {
                0.0
            }
        }
    }
}

@Serializable
data class PremisesDto(
    val id: String = "",
    val organizationId: String = "",
    @SerialName("name")
    val premisesName: String = "",
    val address: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val latitude: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val longitude: Double = 0.0,
    @SerialName("radiusMeters")
    val geofenceRadiusMeters: Int = 50,
    val isActive: Boolean = true,
    val createdAt: String? = null
) {
    val name: String get() = premisesName.ifBlank { "Main Premises" }
    val radiusMeters: Int get() = geofenceRadiusMeters
}

@Serializable
data class CreatePremisesRequest(
    @SerialName("name")
    val premisesName: String = "",
    val address: String? = null,
    val latitude: String = "0.0",
    val longitude: String = "0.0",
    @SerialName("radiusMeters")
    val geofenceRadiusMeters: Int = 50
) {
    val name: String get() = premisesName
    val radiusMeters: Int get() = geofenceRadiusMeters
}

@Serializable
data class UpdatePremisesRequest(
    @SerialName("name")
    val premisesName: String? = null,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("radiusMeters")
    val geofenceRadiusMeters: Int? = null,
    val isActive: Boolean? = null
) {
    val name: String? get() = premisesName
    val radiusMeters: Int? get() = geofenceRadiusMeters
}
