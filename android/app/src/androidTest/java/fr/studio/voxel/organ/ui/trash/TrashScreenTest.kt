package fr.studio.voxel.organ.ui.trash

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.trash.TrashScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashScreenTest {

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
    fun trashScreen_displaysGlobalTrashTitle() {
        // Mocker les appels réseau pour la corbeille globale
        MockInterceptor.addMock("/api/projects/trash", "[]", 200)

        composeTestRule.setContent {
            TrashScreen(
                onBack = {}
            )
        }

        // Attendre que la corbeille se charge
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Corbeille globale").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifie qu'on affiche bien le titre de la corbeille globale
        composeTestRule.onNodeWithText("Corbeille globale").assertIsDisplayed()
    }
}
