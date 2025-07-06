

package com.example.royadetect.utils
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _isLoggedIn = MutableStateFlow(isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow(getCurrentUserData())
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    data class UserData(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val phone: String,
        val age: Int,
        val farmerId: String,
        val isActive: Boolean
    )

    fun saveUserSession(userData: UserData) {
        with(sharedPreferences.edit()) {
            putString("user_id", userData.id)
            putString("user_email", userData.email)
            putString("first_name", userData.firstName)
            putString("last_name", userData.lastName)
            putString("phone", userData.phone)
            putInt("age", userData.age)
            putString("farmer_id", userData.farmerId)
            putBoolean("is_active", userData.isActive)
            putBoolean("is_logged_in", true)
            apply()
        }
        _isLoggedIn.value = true
        _currentUser.value = userData
    }

    fun getUserId(): String? {
        return sharedPreferences.getString("user_id", null)
    }

    fun clearSession() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        _isLoggedIn.value = false
        _currentUser.value = null
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun getCurrentUserData(): UserData? {
        return if (isUserLoggedIn()) {
            val userId = sharedPreferences.getString("user_id", null)
            val email = sharedPreferences.getString("user_email", null)
            val firstName = sharedPreferences.getString("first_name", null)
            val lastName = sharedPreferences.getString("last_name", null)
            val phone = sharedPreferences.getString("phone", null)
            val age = sharedPreferences.getInt("age", 0)
            val farmerId = sharedPreferences.getString("farmer_id", null)
            val isActive = sharedPreferences.getBoolean("is_active", false)

            if (userId != null && email != null && firstName != null && lastName != null) {
                UserData(userId, email, firstName, lastName, phone ?: "", age, farmerId ?: "", isActive)
            } else null
        } else null
    }
}