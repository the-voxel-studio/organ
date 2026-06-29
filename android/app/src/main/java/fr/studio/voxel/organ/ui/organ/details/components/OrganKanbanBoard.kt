package fr.studio.voxel.organ.ui.organ.details.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.studio.voxel.organ.R
import fr.studio.voxel.organ.network.services.Task
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrganKanbanBoard(
    tasks: List<Task>,
    activeColumns: List<Triple<String, String, Color>>,
    pagerState: PagerState,
    highlightColor: Color,
    onTaskClick: (String) -> Unit,
    canDragTask: (Task) -> Boolean,
    onTaskDragStart: (Task, Offset, Offset) -> Unit,
    onTaskDrag: (Offset) -> Unit,
    onTaskDragEnd: () -> Unit,
    tabBounds: MutableMap<String, Rect>,
    onPagerBoundsChanged: (Rect) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

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
                    onPagerBoundsChanged(coordinates.boundsInRoot())
                }
        ) { page ->
            val (status, _, color) = activeColumns[page]
            val tasksForStatus = tasks.filter { it.status == status }

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
                                text = "Aucune tâche",
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
                                isDraggable = canDragTask(task),
                                onDragStart = { offset, cardPos ->
                                    onTaskDragStart(task, offset, cardPos)
                                },
                                onDrag = { amount ->
                                    onTaskDrag(amount)
                                },
                                onDragEnd = {
                                    onTaskDragEnd()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
