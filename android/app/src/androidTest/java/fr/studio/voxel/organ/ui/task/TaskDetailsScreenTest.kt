package fr.studio.voxel.organ.ui.task

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.task.TaskDetailsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val projectUuid = "8983a671-64e2-11f1-b79d-12ba6b29231e"
    private val organUuid = "8986367f-64e2-11f1-b79d-12ba6b29231e"
    private val taskUuid = "8993bd81-64e2-11f1-b79d-12ba6b29231e"

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
    fun taskDetailsScreen_successState_displaysTaskProperties() {
        // Enregistrer le mock pour la récupération des détails de la tâche
        val taskDetailsJson = TestAssetHelper.readAsset("task_details.json")
        MockInterceptor.addMock(
            "/api/projects/$projectUuid/organs/$organUuid/tasks/$taskUuid",
            taskDetailsJson,
            200
        )

        composeTestRule.setContent {
            TaskDetailsScreen(
                projectUuid = projectUuid,
                organUuid = organUuid,
                taskUuid = taskUuid,
                onBack = {},
                onSuccess = {}
            )
        }

        // Attendre que les détails de la tâche soient affichés
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Implement all endpoints for Organ management.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier que le titre de la tâche s'affiche
        composeTestRule.onAllNodesWithText("Organ CRUD").onFirst().assertIsDisplayed()

        // Vérifier la description de la tâche
        composeTestRule.onNodeWithText("Implement all endpoints for Organ management.", substring = true).assertIsDisplayed()
    }
}
