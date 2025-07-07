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
    suspend fun register(@Body registerRequest: RegisterRequest): Response<ApiResponse<LoginData>>

    @GET("auth/validate")
    suspend fun validateSession(@Query("user_id") userId: String): Response<ApiResponse<Any>>

    @POST("batches")
    suspend fun createBatch(@Body batchRequest: BatchRequest): Response<ApiResponse<BatchData>>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val first_name: String,
    val last_name: String,
    val age: Int,
    val phone: String,
    val email: String,
    val password: String
)

data class BatchRequest(
    val farmer_id: String,
    val name: String,
    val description: String
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

data class BatchData(
    val id: String,
    val farmer_id: String,
    val name: String,
    val description: String,
    val creation_date: String
)