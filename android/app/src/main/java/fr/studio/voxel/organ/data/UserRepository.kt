package fr.studio.voxel.organ.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.User
import fr.studio.voxel.organ.network.services.UserApiService

object UserRepository {
    private val userService = ApiClient.createService(UserApiService::class.java)
    private val authService = ApiClient.createService(fr.studio.voxel.organ.network.services.AuthApiService::class.java)
    private val tokenStorage = ApiClient.getTokenStorage()

    var currentUser by mutableStateOf<User?>(null)
        private set

    class AutoLoginFailedException : Exception("Inscription réussie, mais la connexion automatique a échoué.")

    suspend fun login(mail: String, password: String): Result<Unit> {
        return try {
            val response = authService.login(fr.studio.voxel.organ.network.services.LoginRequest(mail, password))
            if (response.isSuccessful) {
                fetchCurrentUser()
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val rawError = try {
                    val json = org.json.JSONObject(errorBody ?: "")
                    json.optString("error", json.optString("message", ""))
                } catch (e: Exception) {
                    ""
                }
                val errorMsg = if (!rawError.isNullOrBlank()) {
                    rawError
                } else {
                    fr.studio.voxel.organ.network.getErrorMessageForCode(response.code(), "Une erreur est survenue lors de la connexion")
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(mail: String, firstName: String, lastName: String, password: String): Result<Unit> {
        return try {
            val response = authService.register(
                fr.studio.voxel.organ.network.services.RegisterRequest(email = mail, firstName = firstName, lastName = lastName, password = password)
            )
            if (response.isSuccessful) {
                val loginResponse = authService.login(fr.studio.voxel.organ.network.services.LoginRequest(mail, password))
                if (loginResponse.isSuccessful) {
                    fetchCurrentUser()
                    Result.success(Unit)
                } else {
                    Result.failure(AutoLoginFailedException())
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val rawError = try {
                    val bodyStr = errorBody ?: ""
                    val json = org.json.JSONObject(bodyStr)
                    if (json.has("detail")) {
                        json.optString("detail")
                    } else if (json.has("violations")) {
                        val violations = json.getJSONArray("violations")
                        val messages = mutableListOf<String>()
                        for (i in 0 until violations.length()) {
                            val violation = violations.getJSONObject(i)
                            val field = violation.optString("propertyPath")
                            val title = violation.optString("title")
                            messages.add("$field: $title")
                        }
                        messages.joinToString("\n")
                    } else {
                        json.optString("error", "Erreur lors de l'inscription.")
                    }
                } catch (e: Exception) {
                    try {
                        val jsonArray = org.json.JSONArray(errorBody ?: "")
                        val messages = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val msg = obj.optString("message") ?: obj.optString("title")
                            if (msg.isNotEmpty()) messages.add(msg)
                        }
                        messages.joinToString("\n")
                    } catch (e2: Exception) {
                        "Erreur lors de l'inscription."
                    }
                }

                val parsedError = when(rawError) {
                    "User already exists" -> "Cet utilisateur existe déjà."
                    "Password must be at least 8 characters" -> "Le mot de passe doit faire au moins 8 caractères."
                    else -> rawError
                }
                Result.failure(Exception(parsedError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            val response = authService.googleLogin(fr.studio.voxel.organ.network.services.GoogleLoginRequest(idToken = idToken))
            if (response.isSuccessful) {
                fetchCurrentUser()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erreur d'authentification Google : ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Problème réseau lors de la connexion Google", e))
        }
    }

    var isCheckingSession by mutableStateOf(false)
        private set

    suspend fun checkSession(): Result<User?> {
        val hasToken = try {
            tokenStorage.getBearerToken() != null
        } catch (e: Exception) {
            false
        }

        if (!hasToken) {
            return Result.success(null)
        }

        isCheckingSession = true
        return try {
            val userRes = userService.getCurrentUser()
            if (userRes.isSuccessful) {
                val user = userRes.body()
                currentUser = user
                Result.success(user)
            } else {
                tokenStorage.clear()
                currentUser = null
                Result.failure(Exception("Session expirée"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isCheckingSession = false
        }
    }

    suspend fun fetchCurrentUser(): Result<User> {
        return try {
            val userRes = userService.getCurrentUser()
            if (userRes.isSuccessful) {
                val user = userRes.body() ?: throw Exception("Profil vide")
                currentUser = user
                Result.success(user)
            } else {
                Result.failure(Exception("Erreur profil : ${userRes.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String, email: String): Result<fr.studio.voxel.organ.network.services.UpdateUserResponse> {
        return try {
            val res = userService.updateProfile(fr.studio.voxel.organ.network.services.UpdateUserRequest(firstName, lastName, email))
            if (res.isSuccessful) {
                val body = res.body() ?: throw Exception("Réponse vide")
                currentUser?.let { current ->
                    currentUser = current.copy(
                        firstName = body.user.firstName,
                        lastName = body.user.lastName,
                        email = body.user.email
                    )
                }
                Result.success(body)
            } else {
                val errorMsg = res.errorBody()?.string() ?: ""
                Result.failure(Exception(parseErrorMessage(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<fr.studio.voxel.organ.network.services.MessageResponse> {
        return try {
            val res = userService.updatePassword(fr.studio.voxel.organ.network.services.UpdatePasswordRequest(currentPassword, newPassword))
            if (res.isSuccessful) {
                Result.success(res.body() ?: fr.studio.voxel.organ.network.services.MessageResponse("Mot de passe mis à jour"))
            } else {
                val errorMsg = res.errorBody()?.string() ?: ""
                Result.failure(Exception(parseErrorMessage(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnections(): Result<List<fr.studio.voxel.organ.network.services.UserConnection>> {
        return try {
            val res = userService.getConnections()
            if (res.isSuccessful) {
                Result.success(res.body() ?: emptyList())
            } else {
                Result.failure(Exception("Erreur lors de la récupération des sessions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeConnection(uuid: String): Result<fr.studio.voxel.organ.network.services.MessageResponse> {
        return try {
            val res = userService.revokeConnection(uuid)
            if (res.isSuccessful) {
                Result.success(res.body() ?: fr.studio.voxel.organ.network.services.MessageResponse("Session invalidée"))
            } else {
                Result.failure(Exception("Erreur lors de la déconnexion de la session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<fr.studio.voxel.organ.network.services.DeleteUserResponse> {
        return try {
            val res = userService.deleteAccount()
            if (res.isSuccessful) {
                val body = res.body() ?: throw Exception("Réponse vide")
                clear()
                Result.success(body)
            } else {
                Result.failure(Exception("Erreur lors de la suppression du compte"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val json = com.google.gson.JsonParser.parseString(errorBody)
            if (json.isJsonObject) {
                val obj = json.asJsonObject
                if (obj.has("message")) {
                    obj.get("message").asString
                } else {
                    errorBody
                }
            } else if (json.isJsonArray) {
                val arr = json.asJsonArray
                if (arr.size() > 0 && arr.get(0).isJsonObject) {
                    val obj = arr.get(0).asJsonObject
                    if (obj.has("message")) {
                        obj.get("message").asString
                    } else {
                        errorBody
                    }
                } else {
                    errorBody
                }
            } else {
                errorBody
            }
        } catch (e: Exception) {
            errorBody
        }
    }

    fun setGoogleAuthLinked(linked: Boolean) {
        currentUser = currentUser?.copy(authWithGoogle = linked)
    }

    fun setMockUser(user: User) {
        currentUser = user
    }

    suspend fun linkGoogle(idToken: String): Result<fr.studio.voxel.organ.network.services.LinkGoogleResponse> {
        return try {
            val res = userService.linkGoogleAccount(fr.studio.voxel.organ.network.services.LinkGoogleRequest(idToken = idToken))
            if (res.isSuccessful) {
                val body = res.body() ?: throw Exception("Réponse vide")
                currentUser = currentUser?.copy(
                    email = body.user.email,
                    authWithGoogle = body.user.authWithGoogle
                )
                Result.success(body)
            } else {
                val errorMsg = res.errorBody()?.string() ?: ""
                Result.failure(Exception(parseErrorMessage(errorMsg)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clear() {
        currentUser = null
        try {
            tokenStorage.clear()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
