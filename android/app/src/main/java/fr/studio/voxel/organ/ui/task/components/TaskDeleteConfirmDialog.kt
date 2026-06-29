package fr.studio.voxel.organ.ui.task.components

import androidx.compose.runtime.Composable
import fr.studio.voxel.organ.ui.components.DeleteConfirmDialog

@Composable
fun TaskDeleteConfirmDialog(
    isDeletePermanent: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DeleteConfirmDialog(
        title = if (isDeletePermanent) "Supprimer définitivement" else "Supprimer la tâche",
        message = if (isDeletePermanent)
            "Cette action est irréversible. La tâche et toutes ses données associées seront définitivement perdues."
        else
            "La tâche sera déplacée vers la corbeille de l'Organ.",
        confirmText = "Supprimer",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
