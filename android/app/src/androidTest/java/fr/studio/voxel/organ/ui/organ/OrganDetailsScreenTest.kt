package fr.studio.voxel.organ.ui.organ

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.organ.details.OrganDetailsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val projectUuid = "8983a671-64e2-11f1-b79d-12ba6b29231e"
    private val organUuid = "8986367f-64e2-11f1-b79d-12ba6b29231e"

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
    fun organDetailsScreen_successState_displaysKanbanBoard() {
        // Enregistrer tous les mocks requis pour l'initialisation de l'Organ
        val projectSingleJson = TestAssetHelper.readAsset("project_single.json")
        val organDetailsJson = TestAssetHelper.readAsset("organ_details.json")
        val organPermissionsJson = TestAssetHelper.readAsset("organ_permissions.json")
        val organTasksJson = TestAssetHelper.readAsset("organ_tasks.json")

        MockInterceptor.addMock("/api/projects/$projectUuid", projectSingleJson, 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid", organDetailsJson, 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/permissions", organPermissionsJson, 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/tasks", organTasksJson, 200)

        composeTestRule.setContent {
            OrganDetailsScreen(
                projectUuid = projectUuid,
                organUuid = organUuid,
                onBack = {},
                onEditOrgan = { _, _ -> },
                onSidebarClick = {},
                onTaskClick = {}
            )
        }

        // Attendre et vérifier que le titre de l'organ s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("API Core").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("API Core").assertIsDisplayed()

        // Vérifier que la tâche "Entity Validation" (présente dans organ_tasks.json et status TODO) s'affiche sur le tableau
        composeTestRule.onNodeWithText("Entity Validation").assertIsDisplayed()
    }
}
