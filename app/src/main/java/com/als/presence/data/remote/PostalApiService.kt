package com.als.presence.data.remote

import com.als.presence.data.model.PostalResponseDto
import retrofit2.Response
import retrofit2.http.*

interface PostalApiService {
    @GET("pincode/{pincode}")
    suspend fun lookup(@Path("pincode") pincode: String): Response<List<PostalResponseDto>>
}
