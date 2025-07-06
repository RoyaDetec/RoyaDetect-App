package com.example.royadetect.repository

import com.example.royadetect.network.ApiService
import com.example.royadetect.network.LoginRequest
import com.example.royadetect.network.RegisterRequest
import com.example.royadetect.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionManager: SessionManager
) {
    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            //.baseUrl("https://your-api-base-url.com/api/") // Cambia por tu URL
            .baseUrl("https://royadetect-services-production.up.railway.app/api/")
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
            emit(Result.failure(e))
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        age: Int,
        phone: String,
        email: String,
        password: String,
        //cropName: String? = null,
        //cropArea: Double? = null
    ): Flow<Result<String>> = flow {
        try {
            val request = RegisterRequest(
                firstName = firstName,
                lastName = lastName,
                age = age,
                phone = phone,
                email = email,
                password = password,
                //cropNamecropName = cropName,
               // cropArea = cropArea
            )
            val response = apiService.register(request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.success("Registro exitoso"))
            } else {
                val errorMessage = response.body()?.message ?: "Error en el registro"
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
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
            sessionManager.clearSession()
            emit(Result.success(false))
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn

    fun getCurrentUser(): Flow<SessionManager.UserData?> = sessionManager.currentUser
}