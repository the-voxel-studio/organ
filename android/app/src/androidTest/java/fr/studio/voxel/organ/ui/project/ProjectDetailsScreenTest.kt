package fr.studio.voxel.organ.ui.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.project.details.ProjectScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val projectUuid = "8983a671-64e2-11f1-b79d-12ba6b29231e"

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
    fun projectDetailsScreen_successState_displaysInfoAndOrgans() {
        // Enregistrer la réponse mockée pour les détails du projet
        val projectDetailedJson = TestAssetHelper.readAsset("project_detailed.json")
        MockInterceptor.addMock("/api/projects/$projectUuid/detailed", projectDetailedJson, 200)

        composeTestRule.setContent {
            ProjectScreen(
                projectUuid = projectUuid,
                onSidebarClick = {},
                onBack = {},
                onEditProject = {},
                onCreateOrgan = {},
                onOrganClick = {},
                onTrashClick = {},
                onStatsClick = {}
            )
        }

        // Attendre que la description du projet soit affichée
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Developing the core Organ project management platform.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier que le titre du projet s'affiche
        composeTestRule.onAllNodesWithText("Organ Platform").onFirst().assertIsDisplayed()

        // Vérifier la description du projet
        composeTestRule.onNodeWithText("Developing the core Organ project management platform.", substring = true).assertExists()

        // Vérifier que les organs du projet sont affichés
        composeTestRule.onNodeWithText("API Core").assertIsDisplayed()
        composeTestRule.onNodeWithText("Web Interface").assertIsDisplayed()
    }

    @Test
    fun projectDetailsScreen_accessDenied_showsAlertDialog() {
        // Simuler un accès interdit (403)
        MockInterceptor.addMock("/api/projects/$projectUuid/detailed", "{\"message\":\"Accès refusé\"}", 403)

        var backCalled = false
        composeTestRule.setContent {
            ProjectScreen(
                projectUuid = projectUuid,
                onSidebarClick = {},
                onBack = { backCalled = true },
                onEditProject = {},
                onCreateOrgan = {},
                onOrganClick = {},
                onTrashClick = {},
                onStatsClick = {}
            )
        }

        // Vérifier qu'une boîte de dialogue s'affiche avec le texte d'Accès interdit
        composeTestRule.onNodeWithText("Accès interdit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vous n'avez pas l'autorisation d'accéder à ce projet.").assertIsDisplayed()

        // Clic sur OK doit appeler onBack
        composeTestRule.onNodeWithText("OK").performClick()
        assert(backCalled)
    }
}
