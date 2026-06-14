package fr.studio.voxel.organ.ui.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

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
    fun dashboardScreen_successState_displaysProjectsAndPriorityTasks() {
        // Enregistrer la réponse mockée du Dashboard
        val dashboardJson = TestAssetHelper.readAsset("dashboard.json")
        MockInterceptor.addMock("/api/dashboard", dashboardJson, 200)

        composeTestRule.setContent {
            val navController = rememberNavController()
            Dashboard(navController = navController)
        }

        // Attendre le chargement des données
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Organ Platform").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Vérifier la présence des projets du JSON
        composeTestRule.onAllNodesWithText("Organ Platform").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Marketing 2026").onFirst().assertIsDisplayed()

        // 2. Vérifier la présence des tâches prioritaires
        composeTestRule.onNodeWithText("Valkey Integration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Organ CRUD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly Newsletter").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_errorState_displaysErrorMessage() {
        // Enregistrer une erreur serveur pour le dashboard
        MockInterceptor.addMock("/api/dashboard", "{\"message\":\"Erreur interne du serveur\"}", 500)

        composeTestRule.setContent {
            val navController = rememberNavController()
            Dashboard(navController = navController)
        }

        // Vérifie qu'un message d'erreur est affiché
        composeTestRule.onNodeWithText("Problème de chargement : Erreur interne du serveur", substring = true).assertIsDisplayed()
    }
}
