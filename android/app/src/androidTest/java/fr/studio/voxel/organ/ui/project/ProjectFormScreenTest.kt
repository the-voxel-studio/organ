package fr.studio.voxel.organ.ui.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.project.form.ProjectFormScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectFormScreenTest {

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
    fun projectFormScreen_creationMode_displaysEmptyForm() {
        composeTestRule.setContent {
            ProjectFormScreen(
                projectUuid = null, // null = Création
                onBack = {},
                onSuccess = {}
            )
        }

        // Attendre que le formulaire se charge et s'affiche
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Créer un Nouveau Projet").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifie qu'on est bien en mode création avec le titre approprié
        composeTestRule.onNodeWithText("Créer un Nouveau Projet").assertIsDisplayed()

        // Saisir un titre de projet dans le champ (Nom du projet)
        composeTestRule.onNodeWithText("Nom du projet").assertIsDisplayed()
    }
}
