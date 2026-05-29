package fr.studio.voxel.organ

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.services.AuthApiService
import fr.studio.voxel.organ.network.services.LoginRequest
import fr.studio.voxel.organ.network.services.ProjectApiService
import fr.studio.voxel.organ.ui.OrganApp
import fr.studio.voxel.organ.ui.theme.OrganTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        enableEdgeToEdge()
        setContent {
            OrganTheme {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    /*ApiTestInterface() //uncomment to test API quickly
                    Spacer(modifier = Modifier.height(16.dp)) */
                    Organ()
                }
            }
        }
    }
}

@Composable
fun ApiTestInterface() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    
    val authService = remember { ApiClient.createService(AuthApiService::class.java) }
    val projectService = remember { ApiClient.createService(ProjectApiService::class.java) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "API Test Status: $status")
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = {
                scope.launch {
                    status = "Logging in..."
                    try {
                        val response = authService.login(LoginRequest("admin@organ.com", "password"))
                        status = if (response.isSuccessful) "Login Success" else "Login Failed: ${response.code()}"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Login")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    status = "Fetching projects..."
                    try {
                        val response = projectService.getProjects()
                        status = if (response.isSuccessful) "Found ${response.body()?.size} projects" else "Fetch Failed: ${response.code()}"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Get Projects")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    status = "Logging out..."
                    try {
                        val response = authService.logout()
                        status = if (response.isSuccessful) "Logout Success" else "Logout Failed: ${response.code()}"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun Organ(modifier: Modifier = Modifier) {
    OrganApp()
}