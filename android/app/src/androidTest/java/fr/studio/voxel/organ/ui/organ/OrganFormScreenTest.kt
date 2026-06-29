package fr.studio.voxel.organ.ui.organ

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.organ.form.OrganFormScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganFormScreenTest {

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
    fun organFormScreen_creationMode_displaysEmptyForm() {
        MockInterceptor.addMock("/api/projects/$projectUuid", "{\"uuid\":\"$projectUuid\", \"title\":\"Organ Platform\"}", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/members", "[]", 200)
        MockInterceptor.addMock("/api/permissions/available", "[]", 200)
        MockInterceptor.addMock("/api/projects/$projectUuid/permissions", "{\"role\":\"ADMIN\"}", 200)

        composeTestRule.setContent {
            OrganFormScreen(
                projectUuid = projectUuid,
                organUuid = null, // null = Création
                onBack = {},
                onSuccess = { _, _ -> }
            )
        }

        // Attendre que le formulaire s'affiche après chargement des données
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Créer un nouvel Organ").fetchSemanticsNodes().isNotEmpty()
        }

        // Vérifier le titre de création d'organ
        composeTestRule.onNodeWithText("Créer un nouvel Organ").assertIsDisplayed()

        // Saisir un nom d'organ dans le champ
        composeTestRule.onNodeWithText("Nom de l'Organ").assertIsDisplayed()
    }
}
