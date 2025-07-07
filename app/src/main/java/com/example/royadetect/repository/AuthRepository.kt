package com.example.royadetect.repository

import com.example.royadetect.network.ApiService
import com.example.royadetect.network.BatchRequest
import com.example.royadetect.network.LoginRequest
import com.example.royadetect.network.RegisterRequest
import com.example.royadetect.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionManager: SessionManager
) {

    private val apiService: ApiService by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }

        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://royadetect-services-production.up.railway.app/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    suspend fun login(email: String, password: String): Flow<Result<String>> = flow {
        try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val loginData = response.body()?.data
                if (loginData != null) {
                    val userData = SessionManager.UserData(
                        id = loginData.id,
                        email = loginData.email,
                        firstName = loginData.farmer.first_name,
                        lastName = loginData.farmer.last_name,
                        phone = loginData.farmer.phone,
                        age = loginData.farmer.age,
                        farmerId = loginData.farmer.id,
                        isActive = loginData.active
                    )
                    sessionManager.saveUserSession(userData)
                    emit(Result.success("Login exitoso"))
                } else {
                    emit(Result.failure(Exception("Error en los datos del usuario")))
                }
            } else {
                val errorMessage = response.body()?.message ?: "Error de autenticación"
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            emit(Result.failure(e))
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        age: Int,
        phone: String,
        email: String,
        password: String
    ): Flow<Result<String>> = flow {
        try {
            Log.d("AuthRepository", "Creating register request for: $email")

            val request = RegisterRequest(
                first_name = firstName,
                last_name = lastName,
                age = age,
                phone = phone,
                email = email,
                password = password
            )

            Log.d("AuthRepository", "Sending register request: $request")
            val response = apiService.register(request)

            Log.d("AuthRepository", "Register response received: ${response.code()}")
            Log.d("AuthRepository", "Response body: ${response.body()}")
            Log.d("AuthRepository", "Response headers: ${response.headers()}")

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("AuthRepository", "Registration successful")
                emit(Result.success("Registro exitoso"))
            } else {
                // Intenta obtener el error del body, si no del errorBody
                val errorMessage = if (response.body()?.message != null) {
                    response.body()?.message!!
                } else {
                    // Si el body es null, trata de leer el errorBody
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Error body: $errorBody")
                    errorBody ?: "Error en el registro (${response.code()})"
                }

                Log.e("AuthRepository", "Registration failed: $errorMessage")
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception during registration", e)
            emit(Result.failure(Exception("Error en el registro: ${e.message}")))
        }
    }

    suspend fun validateSession(): Flow<Result<Boolean>> = flow {
        try {
            val userId = sessionManager.getUserId()
            if (userId != null) {
                val response = apiService.validateSession(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    emit(Result.success(true))
                } else {
                    sessionManager.clearSession()
                    emit(Result.success(false))
                }
            } else {
                emit(Result.success(false))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Session validation error", e)
            sessionManager.clearSession()
            emit(Result.success(false))
        }
    }

    suspend fun createBatch(
        farmerId: String,
        name: String,
        description: String
    ): Flow<Result<String>> = flow {
        try {
            Log.d("AuthRepository", "Creating batch for farmer: $farmerId")

            val request = BatchRequest(
                farmer_id = farmerId,
                name = name,
                description = description
            )

            Log.d("AuthRepository", "Sending batch request: $request")
            val response = apiService.createBatch(request)

            Log.d("AuthRepository", "Batch response received: ${response.code()}")
            Log.d("AuthRepository", "Response body: ${response.body()}")

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("AuthRepository", "Batch created successfully")
                emit(Result.success("Batch creado exitosamente"))
            } else {
                val errorMessage = if (response.body()?.message != null) {
                    response.body()?.message!!
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("AuthRepository", "Error body: $errorBody")
                    errorBody ?: "Error al crear el batch (${response.code()})"
                }

                Log.e("AuthRepository", "Batch creation failed: $errorMessage")
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception during batch creation", e)
            emit(Result.failure(Exception("Error al crear el batch: ${e.message}")))
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn

    fun getCurrentUser(): Flow<SessionManager.UserData?> = sessionManager.currentUser
}