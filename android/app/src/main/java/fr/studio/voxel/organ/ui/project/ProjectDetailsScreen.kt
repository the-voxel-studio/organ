package fr.studio.voxel.organ.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.ProjectDetailedActivity
import fr.studio.voxel.organ.network.services.ProjectDetailedOrgan
import fr.studio.voxel.organ.network.services.ProjectDetailedViewResponse
import fr.studio.voxel.organ.ui.components.PrimaryButton
import fr.studio.voxel.organ.ui.components.ColorSelector
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.ProjectDetailsViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.toArgb

@Composable
fun ProjectDetailsScreen(
    projectUuid: String,
    onSidebarClick: () -> Unit,
    onBack: () -> Unit,
    onEditProject: (String) -> Unit,
    onCreateOrgan: (String) -> Unit,
    viewModel: ProjectDetailsViewModel = viewModel()
) {
    LaunchedEffect(projectUuid) {
        viewModel.initProject(projectUuid)
    }

    var showNotImplementedFeature by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingOverlay(
            isLoading = viewModel.isLoading || viewModel.isSavingTag || viewModel.isTagsLoading,
            text = when {
                viewModel.isLoading -> "Chargement du projet..."
                viewModel.isSavingTag -> "Enregistrement du tag..."
                viewModel.isTagsLoading -> "Chargement des tags..."
                else -> null
            }
        ) {
            if (viewModel.isLoading && viewModel.projectData == null) {
                Box(modifier = Modifier.fillMaxSize())
            } else if (viewModel.errorMessage != null) {
                ProjectErrorState(
                    message = viewModel.errorMessage ?: "",
                    onBack = onBack
                )
            } else {
                viewModel.projectData?.let { data ->
                val projectColor = remember(data.project.color) {
                    try {
                        val hex = data.project.color?.replace("#", "") ?: "FF7EB6"
                        Color(android.graphics.Color.parseColor("#$hex"))
                    } catch (e: Exception) {
                        Color(0xFFFF7EB6)
                    }
                }

                val role = data.project.role ?: "MEMBER"
                val canManage = role == "ADMIN" || role == "MANAGER"

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Header(navigateUp = onSidebarClick, canOpenSidebar = true)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // Project Header Info
                            ProjectHeaderSection(
                                data = data,
                                projectColor = projectColor,
                                canManage = canManage,
                                onEditClick = { onEditProject(projectUuid) },
                                onFeaturePlaceholderClick = { showNotImplementedFeature = it }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Organs Section
                            OrgansSection(
                                organs = data.organs,
                                projectUuid = projectUuid,
                                projectColor = projectColor,
                                canManage = canManage,
                                showTagsPanel = viewModel.showTagsPanel,
                                onToggleTagsClick = { viewModel.toggleTagsPanel() },
                                onOrganClick = { organUuid -> showNotImplementedFeature = "Tâches de l'organe" },
                                tagsPanel = {
                                    ProjectTagsPanel(
                                        projectUuid = projectUuid,
                                        canManage = canManage,
                                        projectColor = projectColor,
                                        showTagsPanel = viewModel.showTagsPanel,
                                        tags = viewModel.tags,
                                        isTagsLoading = viewModel.isTagsLoading,
                                        isSavingTag = viewModel.isSavingTag,
                                        tagErrorMessage = viewModel.tagErrorMessage,
                                        onCreateTag = { name, color -> viewModel.createTag(name, color) },
                                        onUpdateTag = { uuid, name, color -> viewModel.updateTag(uuid, name, color) },
                                        onDeleteTag = { uuid -> viewModel.deleteTag(uuid) }
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // About Section
                            AboutSection(
                                description = data.project.description,
                                createdAt = data.project.createdAt
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Activity Feed Section
                            ActivityFeedSection(
                                activities = data.activities,
                                projectColor = projectColor
                            )

                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    if (canManage) {
                        Button(
                            onClick = { onCreateOrgan(projectUuid) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = projectColor,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add_logo),
                                contentDescription = "Ajouter un nouvel organe",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Modal popup matching Angular's "showNotImplementedAlert"
    showNotImplementedFeature?.let { featureName ->
        AlertDialog(
            onDismissRequest = { showNotImplementedFeature = null },
            title = {
                Text(
                    text = "Fonctionnalité en cours",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "La fonctionnalité \"$featureName\" n'est pas encore disponible dans cette version mobile. Elle sera transposée prochainement !"
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotImplementedFeature = null }) {
                    Text("Compris")
                }
            }
        )
    }
}

@Composable
fun ProjectDetailIconBadge(
    iconType: String?,
    iconData: String?,
    projectColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(if (iconType != null && iconData != null) MaterialTheme.colorScheme.surface else projectColor)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (iconType != null && iconData != null) {
            when (iconType) {
                "EMOJI" -> {
                    Text(
                        text = iconData,
                        fontSize = (size.value * 0.55).sp
                    )
                }
                "SVG" -> {
                    AsyncImage(
                        model = iconData.toByteArray(),
                        contentDescription = "SVG Icon",
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
                "IMAGE", "BLOB" -> {
                    AsyncImage(
                        model = iconData,
                        contentDescription = "Image Icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // Empty colored square fallback
                }
            }
        }
    }
}

@Composable
fun ProjectHeaderSection(
    data: ProjectDetailedViewResponse,
    projectColor: Color,
    canManage: Boolean,
    onEditClick: () -> Unit,
    onFeaturePlaceholderClick: (String) -> Unit
) {
    val project = data.project

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProjectDetailIconBadge(
                    iconType = project.iconType,
                    iconData = project.iconData,
                    projectColor = projectColor,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusLabel = when (project.status) {
                            "ACTIVE" -> "Actif"
                            "ARCHIVED" -> "Archivé"
                            else -> "Inactif"
                        }
                        val statusBg = when (project.status) {
                            "ACTIVE" -> projectColor.copy(alpha = 0.15f)
                            "ARCHIVED" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                        }
                        val statusColor = when (project.status) {
                            "ACTIVE" -> projectColor
                            "ARCHIVED" -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> Color(0xFFD97706)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }

                        Text(
                            text = "Rôle: ${project.role}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            data.admin?.let { admin ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Administrateur: ${admin.firstName} ${admin.lastName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = admin.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Quick actions line
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canManage) {
                    OutlinedButton(
                        onClick = { onFeaturePlaceholderClick("Corbeille") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Corbeille",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (canManage) {
                    OutlinedButton(
                        onClick = { onFeaturePlaceholderClick("Statistiques") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = projectColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_info),
                            contentDescription = "Statistiques",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (project.role == "ADMIN") {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrgansSection(
    organs: List<ProjectDetailedOrgan>,
    projectUuid: String,
    projectColor: Color,
    canManage: Boolean,
    showTagsPanel: Boolean,
    onToggleTagsClick: () -> Unit,
    onOrganClick: (String) -> Unit,
    tagsPanel: @Composable () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(projectColor.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_info),
                        contentDescription = null,
                        tint = projectColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Organes du projet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleTagsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (showTagsPanel) projectColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_tag),
                        contentDescription = "Gérer les tags",
                        modifier = Modifier.size(20.dp),
                        tint = if (showTagsPanel) projectColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Render the tags panel between header and organs list container
        tagsPanel()

        if (organs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun organe n'a encore été créé pour ce projet.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                organs.forEach { organ ->
                    OrganItemCard(
                        organ = organ,
                        projectColor = projectColor,
                        onClick = { onOrganClick(organ.uuid) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrganItemCard(
    organ: ProjectDetailedOrgan,
    projectColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                val organColor = remember(organ.highlightColor) {
                    try {
                        val hex = organ.highlightColor?.replace("#", "") ?: ""
                        if (hex.isNotEmpty()) Color(android.graphics.Color.parseColor("#$hex")) else projectColor
                    } catch (e: Exception) {
                        projectColor
                    }
                }

                ProjectDetailIconBadge(
                    iconType = organ.iconType,
                    iconData = organ.iconData,
                    projectColor = organColor,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = organ.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = organ.description ?: "Aucune description fournie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val count = organ.activeTasksCount
                    Text(
                        text = "$count active${if (count > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Voir les tâches",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun AboutSection(
    description: String?,
    createdAt: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_info),
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "À propos du projet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = description ?: "Aucune description fournie.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Créé le",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = formatDate(createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityFeedSection(
    activities: List<ProjectDetailedActivity>,
    projectColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4ADE80).copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_info),
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Dernières activités",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune activité récente sur les tâches.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    activities.forEachIndexed { index, activity ->
                        ActivityItem(
                            activity = activity,
                            projectColor = projectColor
                        )
                        if (index < activities.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: ProjectDetailedActivity,
    projectColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // User initials
        val initials = remember(activity.userFirstName, activity.userLastName) {
            val f = activity.userFirstName?.firstOrNull()?.uppercase() ?: ""
            val l = activity.userLastName?.firstOrNull()?.uppercase() ?: ""
            if (f.isNotEmpty() || l.isNotEmpty()) "$f$l" else "U"
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = projectColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val userFullName = "${activity.userFirstName ?: ""} ${activity.userLastName ?: ""}".trim()
            val text = getActivityText(activity.type, activity.actionType, activity.fieldName)
            val translatedField = translateField(activity.fieldName).lowercase()

            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    append(userFullName)
                    addStyle(
                        style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                        start = 0,
                        end = userFullName.length
                    )
                    append(" $text")
                    if (activity.fieldName != null && activity.type == "HISTORY" && activity.actionType == "UPDATE") {
                        append(" $translatedField")
                    }
                    if (activity.taskTitle != null) {
                        append(" sur ")
                        val start = length
                        append(activity.taskTitle)
                        addStyle(
                            style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                            start = start,
                            end = length
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activity.organTitle != null) {
                Text(
                    text = "dans ${activity.organTitle}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatTime(activity.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )

                val typeLabel = when (activity.type) {
                    "COMMENT" -> "Commentaire"
                    "ATTACHMENT" -> "Fichier"
                    else -> "Activité"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(projectColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = projectColor,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectErrorState(
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_info),
            contentDescription = "Erreur",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Une erreur est survenue",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = "Retour au tableau de bord",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

// Helpers
fun formatDate(isoDate: String?): String {
    if (isoDate == null) return "-"
    return try {
        val clean = isoDate.substringBefore("T")
        val parts = clean.split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            clean
        }
    } catch (e: Exception) {
        "-"
    }
}

fun formatTime(isoDate: String?): String {
    if (isoDate == null) return ""
    return try {
        val timePart = isoDate.substringAfter("T").substringBefore(".")
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            "${parts[0]}:${parts[1]}"
        } else {
            timePart
        }
    } catch (e: Exception) {
        ""
    }
}

fun getActivityText(type: String, actionType: String?, fieldName: String?): String {
    if (type == "COMMENT") {
        return "a ajouté un commentaire"
    } else if (type == "ATTACHMENT") {
        return "a ajouté une pièce jointe"
    } else if (type == "HISTORY") {
        return when (actionType) {
            "CREATE" -> "a créé la tâche"
            "UPDATE" -> {
                when (fieldName) {
                    "status" -> "a changé le statut"
                    "priority" -> "a changé la priorité"
                    else -> "a modifié ${translateField(fieldName)}"
                }
            }
            "STATUS_CHANGE" -> "a changé le statut"
            "PRIORITY_CHANGE" -> "a changé la priorité"
            "COMMENT_ADD", "COMMENT_CREATE" -> "a ajouté un commentaire"
            "ATTACHMENT_ADD" -> "a ajouté une pièce jointe"
            "LINK_ADD" -> "a ajouté un lien"
            "ASSIGNEE_ADD" -> "a assigné la tâche"
            "ASSIGNEE_REMOVE" -> "a retiré l'assignation"
            "RESTORE" -> "a restauré"
            else -> "a mis à jour la tâche"
        }
    }
    return "a modifié la tâche"
}

fun translateField(field: String?): String {
    if (field == null) return ""
    return when (field) {
        "title" -> "le titre"
        "description" -> "la description"
        "status", "status_message", "statusMessage" -> "le statut"
        "priority" -> "la priorité"
        "estimated_hours", "estimatedHours" -> "le temps estimé"
        "start_date", "startDate" -> "la date de début"
        "expires_at", "expiresAt" -> "l'échéance"
        else -> field
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectTagsPanel(
    projectUuid: String,
    canManage: Boolean,
    projectColor: Color,
    showTagsPanel: Boolean,
    tags: List<fr.studio.voxel.organ.network.services.TagResponse>,
    isTagsLoading: Boolean,
    isSavingTag: Boolean,
    tagErrorMessage: String?,
    onCreateTag: (String, String) -> Unit,
    onUpdateTag: (String, String, String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    if (!showTagsPanel) return

    var showForm by remember { mutableStateOf(false) }
    var editingTagUuid by remember { mutableStateOf<String?>(null) }
    var tagName by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf(Color(0xFF808080)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (canManage) "Gestion des Tags" else "Tags du projet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (canManage) {
                    TextButton(onClick = {
                        showForm = !showForm
                        editingTagUuid = null
                        tagName = ""
                        tagColor = Color(0xFF808080)
                    }) {
                        Text(
                            text = if (showForm) "Fermer le formulaire" else "+ Ajouter un tag",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = projectColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isTagsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = projectColor, modifier = Modifier.size(24.dp))
                }
            } else {
                tagErrorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (tags.isEmpty()) {
                    Text(
                        text = "Aucun tag disponible.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tags.forEach { tag ->
                            val tColor = remember(tag.color) {
                                try {
                                    val hex = tag.color.replace("#", "")
                                    Color(android.graphics.Color.parseColor("#$hex"))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (canManage) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                editingTagUuid = tag.uuid
                                                tagName = tag.name
                                                tagColor = tColor
                                                showForm = true
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                onDeleteTag(tag.uuid)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Creation / Edit Form
            if (showForm && canManage) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (editingTagUuid != null) "Modifier le tag" else "Nouveau tag",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    placeholder = { Text("Ex: Urgent, Bug...") },
                    singleLine = true,
                    label = { Text("Nom du tag") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Couleur du tag",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                ColorSelector(
                    selectedColor = tagColor,
                    onColorSelected = { tagColor = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        showForm = false
                        editingTagUuid = null
                        tagName = ""
                    }) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val colorHex = String.format("#%06X", 0xFFFFFF and tagColor.toArgb())
                            if (tagName.isNotBlank()) {
                                val uuid = editingTagUuid
                                if (uuid != null) {
                                    onUpdateTag(uuid, tagName, colorHex)
                                } else {
                                    onCreateTag(tagName, colorHex)
                                }
                                showForm = false
                                editingTagUuid = null
                                tagName = ""
                            }
                        },
                        enabled = tagName.isNotBlank() && !isSavingTag,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingTag) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}
