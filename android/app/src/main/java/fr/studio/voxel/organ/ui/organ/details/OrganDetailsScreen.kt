package fr.studio.voxel.organ.ui.organ.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.ui.components.LoadingOverlay
import fr.studio.voxel.organ.ui.header.Header
import fr.studio.voxel.organ.viewmodel.OrganDetailsViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

// Sub-components imports
import fr.studio.voxel.organ.ui.organ.details.components.OrganHeaderSection
import fr.studio.voxel.organ.ui.organ.details.components.OrganActionsSection
import fr.studio.voxel.organ.ui.organ.details.components.OrganLinksPanel
import fr.studio.voxel.organ.ui.organ.details.components.OrganFiltersRow
import fr.studio.voxel.organ.ui.organ.details.components.TaskCard
import fr.studio.voxel.organ.ui.organ.details.components.OrganErrorState

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OrganDetailsScreen(
    projectUuid: String,
    organUuid: String,
    onBack: () -> Unit,
    onEditOrgan: (String, String) -> Unit,
    onSidebarClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: OrganDetailsViewModel = viewModel()
) {
    LaunchedEffect(projectUuid, organUuid) {
        viewModel.initOrgan(projectUuid, organUuid)
    }

    val context = LocalContext.current
    var isDragging by remember { mutableStateOf(false) }
    var draggedTask by remember { mutableStateOf<fr.studio.voxel.organ.network.services.Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragInitialPosition by remember { mutableStateOf(Offset.Zero) }
    val dragPosition by remember { derivedStateOf { dragInitialPosition + dragOffset } }
    val tabBounds = remember { mutableMapOf<String, Rect>() }

    var showNotImplementedFeature by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val projectColor = remember(viewModel.projectColor) {
        try {
            val hex = viewModel.projectColor.replace("#", "")
            Color(android.graphics.Color.parseColor("#$hex"))
        } catch (e: Exception) {
            Color(0xFFFF7EB6)
        }
    }

    val highlightColor = remember(viewModel.organData?.highlightColor, projectColor) {
        try {
            val hex = viewModel.organData?.highlightColor?.replace("#", "") ?: ""
            if (hex.isNotEmpty()) Color(android.graphics.Color.parseColor("#$hex"))
            else projectColor
        } catch (e: Exception) {
            projectColor
        }
    }

    val activeColumns = remember(viewModel.selectedStatuses, highlightColor) {
        val allCols = listOf(
            Triple("TODO", "À Faire", Color(0xFF94A3B8)),
            Triple("IN_PROGRESS", "En Cours", highlightColor),
            Triple("WAITING", "En Attente", Color(0xFFFBBF24)),
            Triple("DONE", "Terminé", Color(0xFF4ADE80)),
            Triple("CANCELED", "Annulé", Color(0xFFFB7185))
        )
        if (viewModel.selectedStatuses.isEmpty()) allCols
        else allCols.filter { viewModel.selectedStatuses.contains(it.first) }
    }

    val pagerState = rememberPagerState { activeColumns.size }
    var pagerBounds by remember { mutableStateOf<Rect?>(null) }

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val edgeThresholdPx = with(density) { 45.dp.toPx() }

    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        var leftHoverStart = 0L
        var rightHoverStart = 0L
        val hoverDelayMs = 1000L

        while (isDragging) {
            val currentX = dragPosition.x
            val now = System.currentTimeMillis()

            if (currentX < edgeThresholdPx) {
                rightHoverStart = 0L
                if (leftHoverStart == 0L) {
                    leftHoverStart = now
                } else if (now - leftHoverStart >= hoverDelayMs) {
                    if (pagerState.currentPage > 0) {
                        val targetPage = pagerState.currentPage - 1
                        pagerState.animateScrollToPage(targetPage)
                        leftHoverStart = System.currentTimeMillis()
                    }
                }
            } else if (currentX > screenWidthPx - edgeThresholdPx) {
                leftHoverStart = 0L
                if (rightHoverStart == 0L) {
                    rightHoverStart = now
                } else if (now - rightHoverStart >= hoverDelayMs) {
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        val targetPage = pagerState.currentPage + 1
                        pagerState.animateScrollToPage(targetPage)
                        rightHoverStart = System.currentTimeMillis()
                    }
                }
            } else {
                leftHoverStart = 0L
                rightHoverStart = 0L
            }

            kotlinx.coroutines.delay(100)
        }
    }

    val configuration = LocalConfiguration.current
    val tasksHeight = remember(configuration.screenHeightDp) {
        val height = configuration.screenHeightDp - 140
        if (height < 350) 350.dp else height.dp
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoadingOverlay(
            isLoading = viewModel.isLoading || viewModel.isSavingLink || viewModel.isLinksLoading,
            text = when {
                viewModel.isLoading -> "Chargement de l'Organ..."
                viewModel.isSavingLink -> "Enregistrement du lien..."
                viewModel.isLinksLoading -> "Chargement des liens..."
                else -> null
            }
        ) {
            if (viewModel.isLoading && viewModel.organData == null) {
                Box(modifier = Modifier.fillMaxSize())
            } else if (viewModel.errorMessage != null) {
                OrganErrorState(
                    message = viewModel.errorMessage ?: "",
                    onBack = onBack
                )
            } else {
                viewModel.organData?.let { organ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Glow
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = highlightColor.copy(alpha = 0.04f),
                                radius = 260.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x = size.width, y = 0f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Header(navigateUp = onSidebarClick, canOpenSidebar = true)

                            // Scrollable Content Panel (Organ info, links, filters, and tasks)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {

                                // Organ Header Details Card
                                OrganHeaderSection(
                                    organ = organ,
                                    projectTitle = viewModel.projectTitle,
                                    projectColor = projectColor,
                                    highlightColor = highlightColor,
                                    canManage = viewModel.hasPermission("ORGAN_EDIT") ||
                                            viewModel.hasPermission("ORGAN_MANAGE_MEMBERS") ||
                                            viewModel.hasPermission("ORGAN_MANAGE_ROLES"),
                                    canViewTrash = viewModel.hasPermission("ORGAN_EDIT") ||
                                            viewModel.hasPermission("TASK_DELETE_ALL"),
                                    onEditClick = { onEditOrgan(projectUuid, organ.uuid) },
                                    onTrashClick = { showNotImplementedFeature = "Corbeille des tâches" }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons and View Commutation Row
                                OrganActionsSection(
                                    highlightColor = highlightColor,
                                    activeView = viewModel.activeView,
                                    onViewChange = { viewModel.activeView = it },
                                    showLinks = viewModel.showLinksPanel,
                                    onToggleLinks = { viewModel.toggleLinksPanel() }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Links panel
                                AnimatedVisibility(
                                    visible = viewModel.showLinksPanel,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    OrganLinksPanel(
                                        projectUuid = projectUuid,
                                        organUuid = organ.uuid,
                                        canManage = viewModel.hasPermission("ORGAN_LINK_MANAGE"),
                                        highlightColor = highlightColor,
                                        showLinksPanel = true,
                                        links = viewModel.links,
                                        isLinksLoading = viewModel.isLinksLoading,
                                        isSavingLink = viewModel.isSavingLink,
                                        linkErrorMessage = viewModel.linkErrorMessage,
                                        onCreateLink = { url, desc -> viewModel.createLink(url, desc) },
                                        onUpdateLink = { uuid, url, desc -> viewModel.updateLink(uuid, url, desc) },
                                        onDeleteLink = { uuid -> viewModel.deleteLink(uuid) }
                                    )
                                }

                                // Filters bar (Unified Card Row)
                                OrganFiltersRow(
                                    viewModel = viewModel,
                                    highlightColor = highlightColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tasks rendering section
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(tasksHeight)
                                ) {
                                    if (viewModel.activeView == "kanban") {
                                        if (activeColumns.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Aucun statut sélectionné",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        } else {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                ScrollableTabRow(
                                                    selectedTabIndex = pagerState.currentPage,
                                                    edgePadding = 0.dp,
                                                    containerColor = Color.Transparent,
                                                    contentColor = highlightColor,
                                                    divider = {},
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    activeColumns.forEachIndexed { index, (status, label, color) ->
                                                        Tab(
                                                            selected = pagerState.currentPage == index,
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    pagerState.animateScrollToPage(index)
                                                                }
                                                            },
                                                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                                                tabBounds[status] = coordinates.boundsInRoot()
                                                            },
                                                            text = {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(8.dp)
                                                                            .clip(CircleShape)
                                                                            .background(color)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = label,
                                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                HorizontalPager(
                                                    state = pagerState,
                                                    beyondViewportPageCount = activeColumns.size,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .onGloballyPositioned { coordinates ->
                                                            pagerBounds = coordinates.boundsInRoot()
                                                        }
                                                ) { page ->
                                                    val (status, _, color) = activeColumns[page]
                                                    val tasksForStatus = viewModel.filteredTasks.filter { it.status == status }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .heightIn(min = 350.dp)
                                                            .clip(RoundedCornerShape(32.dp))
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        highlightColor.copy(alpha = 0.08f),
                                                                        highlightColor.copy(alpha = 0.02f)
                                                                    )
                                                                )
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = highlightColor.copy(alpha = 0.1f),
                                                                shape = RoundedCornerShape(32.dp)
                                                            )
                                                            .padding(16.dp)
                                                    ) {
                                                        if (tasksForStatus.isEmpty()) {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Column(
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(id = R.drawable.outline_add_circle_24),
                                                                        contentDescription = null,
                                                                        tint = color.copy(alpha = 0.35f),
                                                                        modifier = Modifier.size(48.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.height(12.dp))
                                                                    Text(
                                                                        "Aucune tâche",
                                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                                            fontWeight = FontWeight.Bold
                                                                        ),
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            LazyColumn(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentPadding = PaddingValues(bottom = 88.dp),
                                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                                            ) {
                                                                 items(tasksForStatus, key = { it.uuid }) { task ->
                                                                     TaskCard(
                                                                         task = task,
                                                                         highlightColor = highlightColor,
                                                                         onClick = { onTaskClick(task.uuid) },
                                                                         isDraggable = viewModel.canDragTask(task),
                                                                         onDragStart = { offset, cardPos ->
                                                                             dragInitialPosition = cardPos + offset
                                                                             dragOffset = Offset.Zero
                                                                             draggedTask = task
                                                                             isDragging = true
                                                                         },
                                                                         onDrag = { amount ->
                                                                             dragOffset += amount
                                                                         },
                                                                         onDragEnd = {
                                                                             if (isDragging && draggedTask != null) {
                                                                                 var targetColumn = activeColumns.find { col ->
                                                                                     val bounds = tabBounds[col.first]
                                                                                     bounds?.contains(dragPosition) == true
                                                                                 }
                                                                                 if (targetColumn == null && pagerBounds?.contains(dragPosition) == true) {
                                                                                     if (pagerState.currentPage in activeColumns.indices) {
                                                                                         targetColumn = activeColumns[pagerState.currentPage]
                                                                                     }
                                                                                 }
                                                                                 if (targetColumn != null && targetColumn.first != draggedTask!!.status) {
                                                                                     val targetStatus = targetColumn.first
                                                                                     if (viewModel.canChangeStatus(draggedTask!!, targetStatus)) {
                                                                                         viewModel.updateTaskStatus(draggedTask!!, targetStatus)
                                                                                         android.widget.Toast.makeText(
                                                                                             context,
                                                                                             "Statut mis à jour : ${targetColumn.second}",
                                                                                             android.widget.Toast.LENGTH_SHORT
                                                                                         ).show()
                                                                                     } else {
                                                                                         android.widget.Toast.makeText(
                                                                                             context,
                                                                                             "Action non autorisée : permissions insuffisantes",
                                                                                             android.widget.Toast.LENGTH_SHORT
                                                                                         ).show()
                                                                                     }
                                                                                 }
                                                                             }
                                                                             isDragging = false
                                                                             draggedTask = null
                                                                         }
                                                                     )
                                                                 }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val tasks = viewModel.filteredTasks
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(32.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            highlightColor.copy(alpha = 0.08f),
                                                            highlightColor.copy(alpha = 0.02f)
                                                        )
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = highlightColor.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(32.dp)
                                                )
                                                .padding(16.dp)
                                        ) {
                                            if (tasks.isEmpty()) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Aucune tâche à afficher",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        ),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(bottom = 88.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                     items(tasks, key = { it.uuid }) { task ->
                                                         TaskCard(
                                                             task = task,
                                                             highlightColor = highlightColor,
                                                             onClick = { onTaskClick(task.uuid) }
                                                         )
                                                     }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Floating Add Button
                        if (viewModel.hasPermission("TASK_CREATE")) {
                            Button(
                                onClick = { showNotImplementedFeature = "Création de tâche" },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = highlightColor,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add_logo),
                                    contentDescription = "Ajouter une tâche",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        // Dragged Floating Task Card overlay
                        if (isDragging && draggedTask != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (dragPosition.x - 150.dp.toPx()).toInt(),
                                                y = (dragPosition.y - 50.dp.toPx()).toInt()
                                            )
                                        }
                                        .width(300.dp)
                                        .graphicsLayer {
                                            scaleX = 1.05f
                                            scaleY = 1.05f
                                            rotationZ = 3f
                                            alpha = 0.9f
                                        }
                                ) {
                                    TaskCard(
                                        task = draggedTask!!,
                                        highlightColor = highlightColor,
                                        onClick = {}
                                    )
                                }
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
