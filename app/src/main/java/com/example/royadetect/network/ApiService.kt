package com.example.royadetect.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<ApiResponse<Any>>

    @GET("auth/validate")
    suspend fun validateSession(@Query("user_id") userId: String): Response<ApiResponse<Any>>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val phone: String,
    val email: String,
    val password: String,
    val cropName: String? = null,
    val cropArea: Double? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class LoginData(
    val id: String,
    val email: String,
    val active: Boolean,
    val creation_date: String,
    val farmer: FarmerData
)

data class FarmerData(
    val id: String,
    val first_name: String,
    val last_name: String,
    val phone: String,
    val age: Int
)