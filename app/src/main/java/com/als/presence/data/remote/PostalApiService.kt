package com.als.presence.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class PostOffice(
    @SerialName("Name") val name: String? = null,
    @SerialName("Description") val description: String? = null,
    @SerialName("BranchType") val branchType: String? = null,
    @SerialName("DeliveryStatus") val deliveryStatus: String? = null,
    @SerialName("Circle") val circle: String? = null,
    @SerialName("District") val district: String? = null,
    @SerialName("Division") val division: String? = null,
    @SerialName("Region") val region: String? = null,
    @SerialName("State") val state: String? = null,
    @SerialName("Country") val country: String? = null,
    @SerialName("Pincode") val pincode: String? = null
)

@Serializable
data class PostalPincodeResponse(
    @SerialName("Message") val message: String? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("PostOffice") val postOffice: List<PostOffice>? = null
)

interface PostalApiService {
    @GET("pincode/{pincode}")
    suspend fun lookupPincode(@Path("pincode") pincode: String): List<PostalPincodeResponse>
}
