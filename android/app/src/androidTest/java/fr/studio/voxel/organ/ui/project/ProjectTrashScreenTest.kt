package fr.studio.voxel.organ.ui.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.project.trash.ProjectTrashScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectTrashScreenTest {

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
    fun projectTrashScreen_displaysTrashTitle() {
        // Mocker les appels réseau pour charger la corbeille de projet
        MockInterceptor.addMock("/api/projects/$projectUuid", "{\"uuid\":\"$projectUuid\", \"title\":\"Organ Platform\"}", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/trash", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/members/trash", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/tags/trash", "[]", 200)

        composeTestRule.setContent {
            ProjectTrashScreen(
                projectUuid = projectUuid,
                onBack = {}
            )
        }

        // Attendre que la corbeille s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Corbeille", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifie qu'on affiche bien le titre de la corbeille du projet
        composeTestRule.onNodeWithText("Corbeille", substring = true).assertIsDisplayed()
    }
}
