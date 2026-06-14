package fr.studio.voxel.organ

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.studio.voxel.organ.network.ApiClient
import fr.studio.voxel.organ.network.MockInterceptor
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.dashboard.Dashboard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class PrimaryButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_displaysTextAndTriggersClick() {
        var clicked = false
        composeTestRule.setContent {
            PrimaryButton(
                text = "Mon Bouton Test",
                onClick = { clicked = true }
            )
        }

        // Vérifie que le bouton affiche le texte
        composeTestRule.onNodeWithText("Mon Bouton Test").assertIsDisplayed()

        // Clique sur le bouton
        composeTestRule.onNodeWithText("Mon Bouton Test").performClick()

        // Vérifie que l'action de clic a bien été appelée
        assertTrue(clicked)
    }

    @Test
    fun primaryButton_whenLoading_doesNotShowText() {
        composeTestRule.setContent {
            PrimaryButton(
                text = "En cours...",
                onClick = {},
                isLoading = true
            )
        }

        // En cours de chargement, le texte est masqué (remplacé par le loader)
        composeTestRule.onNodeWithText("En cours...").assertDoesNotExist()
    }

    @Test
    fun dashboardScreen_displaysMockedProjectsAndTasks() {
        // 1. Initialiser ApiClient avec le contexte de test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ApiClient.init(context)

        // 2. Activer le MockInterceptor
        MockInterceptor.isMockEnabled = true

        // 3. Définir le JSON factice pour /api/dashboard
        val fakeDashboardJson = """
            {
                "projects": [
                    {
                        "id": 1,
                        "uuid": "project-uuid-123",
                        "title": "Projet Test Mock",
                        "description": "Description du projet mocké",
                        "color": "#FF5733",
                        "state": "ACTIVE",
                        "iconType": "EMOJI",
                        "iconData": "🚀"
                    }
                ],
                "tasks": [
                    {
                        "id": 10,
                        "uuid": "task-uuid-456",
                        "organId": 2,
                        "title": "Tâche Test Mock",
                        "description": "Faire les tests de l'application",
                        "priority": 3,
                        "status": "TODO",
                        "projectUuid": "project-uuid-123",
                        "projectName": "Projet Test Mock",
                        "organUuid": "organ-uuid-789",
                        "organName": "Technique"
                    }
                ]
            }
        """.trimIndent()

        MockInterceptor.addMock("/api/dashboard", fakeDashboardJson)

        // 4. Afficher le Dashboard
        composeTestRule.setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            Dashboard(navController = navController)
        }

        // 5. Vérifier que l'écran affiche les données mockées injectées
        composeTestRule.onAllNodesWithText("Projet Test Mock").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Tâche Test Mock").assertIsDisplayed()

        // 6. Nettoyer le MockInterceptor pour ne pas impacter d'autres tests
        MockInterceptor.clearMocks()
    }
}
