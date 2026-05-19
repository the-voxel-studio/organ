package fr.studio.voxel.organ.network.services

import fr.studio.voxel.organ.ViewModel.User
import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("/api/users/me")
    suspend fun getCurrentUser(): Response<User>

    @GET("/api/users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>
}
