package fr.studio.voxel.organ.ui.organ

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.organ.trash.OrganTrashScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganTrashScreenTest {

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
    fun organTrashScreen_displaysTrashTitle() {
        // Mocker les appels d'initialisation de la corbeille de l'organ
        MockInterceptor.addMock("/api/projects/$projectUuid", "{\"uuid\":\"$projectUuid\", \"title\":\"Organ Platform\"}", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid", "{\"uuid\":\"$organUuid\", \"title\":\"API Core\"}", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/permissions", "{\"permissions\":[\"ORGAN_VIEW\"]}", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/tasks/trash", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/links/trash", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/roles/trash", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/organs/$organUuid/roles/members/trash", "[]", 200)

        composeTestRule.setContent {
            OrganTrashScreen(
                projectUuid = projectUuid,
                organUuid = organUuid,
                onBack = {},
                onTaskClick = {}
            )
        }

        // Attendre que la corbeille s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Corbeille", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifie qu'on affiche bien le titre de la corbeille de l'organ
        composeTestRule.onNodeWithText("Corbeille", substring = true).assertIsDisplayed()
    }
}
