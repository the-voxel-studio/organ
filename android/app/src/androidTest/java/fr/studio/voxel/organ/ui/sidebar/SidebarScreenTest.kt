package fr.studio.voxel.organ.ui.sidebar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SidebarScreenTest {

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
    fun sidebarScreen_displaysGeneralMenuOptions() {
        // Mocker les appels d'initialisation de la Sidebar
        MockInterceptor.addMock("/api/projects", "[]", 200)
        MockInterceptor.addMock("/api/notifications", "[]", 200)
        MockInterceptor.addMock("/api/invitations", "[]", 200)

        composeTestRule.setContent {
            val navController = rememberNavController()
            SideBar(
                navController = navController,
                onModifButtonClicked = {}
            )
        }

        // Attendre que la sidebar s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifie qu'on affiche bien les boutons de menu généraux
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("notification").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("poubelle").assertIsDisplayed()
    }
}
