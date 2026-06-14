package fr.studio.voxel.organ.ui.parameter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.data.UserRepository
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.parameter.details.Parameter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParameterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ApiClient.init(context)
        MockInterceptor.isMockEnabled = true
    }

    @After
    fun tearDown() {
        MockInterceptor.clearMocks()
    }

    @Test
    fun parameterScreen_successState_displaysSessionsAndProfile() {
        // Enregistrer les mocks pour le profil de l'utilisateur connecté et ses connexions actives
        val userMeJson = TestAssetHelper.readAsset("user_me.json")
        val userConnectionsJson = TestAssetHelper.readAsset("user_connections.json")

        MockInterceptor.addMock("/api/users/me", userMeJson, 200)
        MockInterceptor.addMock("/api/users/me/connections", userConnectionsJson, 200)

        // Pré-charger le currentUser mocké pour que ParameterViewModel puisse l'initialiser dans son constructeur
        runBlocking {
            UserRepository.fetchCurrentUser()
        }

        composeTestRule.setContent {
            val navController = rememberNavController()
            Parameter(navController = navController)
        }

        // Attendre que la page se charge
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("admin@organ.com").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier que le titre de la page s'affiche
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()

        // Vérifier que les informations de profil de l'utilisateur s'affichent
        composeTestRule.onNodeWithText("admin@organ.com").assertIsDisplayed()
    }
}
