package com.als.presence.domain.usecase.location

import com.als.presence.core.common.Resource
import com.als.presence.data.remote.PostOffice
import com.als.presence.data.remote.PostalApiService
import javax.inject.Inject

class LookupPincodeUseCase @Inject constructor(
    private val postalApiService: PostalApiService
) {
    suspend operator fun invoke(pincode: String): Resource<List<PostOffice>> {
        val cleanPincode = pincode.trim().filter { it.isDigit() }
        if (cleanPincode.length != 6) {
            val msg = "Please enter a valid 6-digit Indian Pincode"
            return Resource.Error(IllegalArgumentException(msg), msg)
        }

        return try {
            val responseList = postalApiService.lookupPincode(cleanPincode)
            val response = responseList.firstOrNull()
            if (response != null && response.status.equals("Success", ignoreCase = true) && !response.postOffice.isNullOrEmpty()) {
                Resource.Success(response.postOffice)
            } else {
                val msg = response?.message ?: "No office details found for Pincode $cleanPincode"
                Resource.Error(IllegalStateException(msg), msg)
            }
        } catch (e: Exception) {
            Resource.Error(e, e.localizedMessage ?: "Failed to connect to Postal API")
        }
    }
}
