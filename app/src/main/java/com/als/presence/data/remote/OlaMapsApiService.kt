package com.als.presence.data.remote

import com.als.presence.data.model.OlaMapsReverseGeocodeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.UUID

interface OlaMapsApiService {
    @GET("places/v1/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("api_key") apiKey: String,
        @Header("X-Request-Id") requestId: String = UUID.randomUUID().toString()
    ): Response<OlaMapsReverseGeocodeResponse>
}
