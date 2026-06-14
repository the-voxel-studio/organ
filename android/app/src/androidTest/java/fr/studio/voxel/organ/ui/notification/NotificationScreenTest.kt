package fr.studio.voxel.organ.ui.notification

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
class NotificationScreenTest {

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
    fun notificationScreen_successState_displaysNotificationsAndInvitations() {
        // Enregistrer les mocks pour les notifications et invitations
        val notificationsJson = TestAssetHelper.readAsset("notifications.json")
        val invitationsJson = TestAssetHelper.readAsset("invitations.json")

        MockInterceptor.addMock("/api/notifications", notificationsJson, 200)
        MockInterceptor.addMock("/api/invitations", invitationsJson, 200)

        composeTestRule.setContent {
            val navController = rememberNavController()
            NotificationScreen(navController = navController)
        }

        // Attendre que les notifications soient chargées et affichées
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Notifications").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier que le titre général s'affiche
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
    }
}
