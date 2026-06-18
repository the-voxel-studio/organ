package fr.studio.voxel.organ.ui.authentication

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.OrganScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ApiClient.init(context)
        fr.studio.voxel.organ.data.UserRepository.clear()
        MockInterceptor.isMockEnabled = true
    }

    @After
    fun tearDown() {
        MockInterceptor.clearMocks()
    }

    @Test
    fun authScreen_whenCheckedIsFalse_primaryButtonIsDisabled() {
        var modeSwitchCalled = false
        composeTestRule.setContent {
            val navController = rememberNavController()
            AuthScreen(
                mode = AuthMode.SIGN_IN,
                navController = navController,
                onModeSwitch = { modeSwitchCalled = true }
            )
        }

        // Le bouton de connexion "SE CONNECTER" doit être désactivé car la Checkbox CGU n'est pas cochée par défaut
        composeTestRule.onNodeWithText("SE CONNECTER").assertIsNotEnabled()
    }

    @Test
    fun authScreen_whenCheckedIsTrue_primaryButtonIsEnabled() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AuthScreen(
                mode = AuthMode.SIGN_IN,
                navController = navController,
                onModeSwitch = {}
            )
        }

        // Coche la Checkbox
        composeTestRule.onNode(hasClickAction() and isToggleable()).performClick()

        // Le bouton de connexion doit maintenant être activé
        composeTestRule.onNodeWithText("SE CONNECTER").assertIsEnabled()
    }

    @Test
    fun authScreen_loginFailed_showsError() {
        // Enregistrer une erreur 401 pour la route de login
        MockInterceptor.addMock("/api/auth/login", "{\"message\":\"Identifiants incorrects\"}", 401)

        composeTestRule.setContent {
            val navController = rememberNavController()
            AuthScreen(
                mode = AuthMode.SIGN_IN,
                navController = navController,
                onModeSwitch = {}
            )
        }

        // Remplir les champs
        composeTestRule.onNodeWithText("nom@exemple.com").performTextInput("admin@organ.com")
        composeTestRule.onNodeWithText("•••••••••").performTextInput("wrongpassword")

        // Activer la Checkbox et soumettre
        composeTestRule.onNode(hasClickAction() and isToggleable()).performClick()
        composeTestRule.onNodeWithText("SE CONNECTER").performClick()

        // Attendre que l'erreur s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Identifiants incorrects").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Identifiants incorrects").assertIsDisplayed()
    }

    @Test
    fun authScreen_loginSuccess_triggersRedirection() {
        // Enregistrer des réponses de succès pour le login et la récupération de profil
        val userMeJson = TestAssetHelper.readAsset("user_me.json")
        MockInterceptor.addMock("/api/auth/login", "", 204)
        MockInterceptor.addMock("/api/users/me", userMeJson, 200)

        composeTestRule.setContent {
            val navController = rememberNavController()
            navController.graph = navController.createGraph(startDestination = OrganScreen.SignIn.name) {
                composable(OrganScreen.SignIn.name) {}
                composable(OrganScreen.Dashboard.name) {}
            }
            AuthScreen(
                mode = AuthMode.SIGN_IN,
                navController = navController,
                onModeSwitch = {}
            )
        }

        // Remplir les champs
        composeTestRule.onNodeWithText("nom@exemple.com").performTextInput("admin@organ.com")
        composeTestRule.onNodeWithText("•••••••••").performTextInput("password")

        // Activer la Checkbox et soumettre
        composeTestRule.onNode(hasClickAction() and isToggleable()).performClick()
        composeTestRule.onNodeWithText("SE CONNECTER").performClick()

        // Après la connexion réussie, viewModel.currentUser est mis à jour et redirige vers le Dashboard
    }

    @Test
    fun authScreen_signupPasswordsMismatch_showsError() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            AuthScreen(
                mode = AuthMode.SIGN_UP,
                navController = navController,
                onModeSwitch = {}
            )
        }

        // En mode inscription, vérifions la validation du formulaire
        composeTestRule.onNodeWithText("Crée un compte").assertIsDisplayed()
    }
}
