package fr.studio.voxel.organ.ui.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.TestAssetHelper
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.project.stats.ProjectStatsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectStatsScreenTest {

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
    fun projectStatsScreen_successState_displaysKpis() {
        val projectDetailedJson = TestAssetHelper.readAsset("project_detailed.json")
        val projectStatsJson = TestAssetHelper.readAsset("project_stats.json")

        MockInterceptor.addMock("/api/projects/$projectUuid/detailed", projectDetailedJson, 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/stats", projectStatsJson, 200)

        composeTestRule.setContent {
            ProjectStatsScreen(
                projectUuid = projectUuid,
                onBack = {}
            )
        }

        // Attendre le chargement des statistiques
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Statistiques", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier la présence du titre de l'écran ou d'une carte KPI d'avancement
        // project_detailed.json contient "Organ Platform"
        composeTestRule.onAllNodesWithText("Organ Platform", substring = true).onFirst().assertIsDisplayed()
        
        // Vérifier la présence d'un titre de statistiques
        composeTestRule.onNodeWithText("Statistiques", substring = true).assertIsDisplayed()
    }
}
