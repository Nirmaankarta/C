package com.nirmaankarta.priority

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val DueColor = Color(0xFFE5484D)
private val IdleIcon = Color(0xFF8A90A0)

/** Bottom-navigation destinations. "More" is not a tab: it opens profile & settings. */
private enum class NavTab(val title: String) {
    Tasks("Tasks"), Recent("Recent"), Calendar("Calendar"), Matrix("Eisenhower Matrix")
}

private fun formatDue(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM"))

// =====================================================================================
// Root: top bar + content + FAB + bottom navigation
// =====================================================================================

@Composable
fun MatrixApp(vm: MatrixViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(NavTab.Matrix.ordinal) }
    var addFor by remember { mutableStateOf<Quadrant?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    val current = NavTab.entries[tab]

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(current.title, vm) { showProfile = true }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (current == NavTab.Matrix) MatrixGrid(vm) { addFor = it }
                else TaskListScreen(current, vm)
                Fab(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { addFor = Quadrant.Q1 }
            }
            BottomBar(current, onSelect = { tab = it.ordinal }, onMore = { showProfile = true })
        }
    }

    addFor?.let { q ->
        AddTaskSheet(
            initial = q,
            onPreview = { vm.play(it, true) },
            onSubmit = { text, quad, due -> vm.addTask(text, quad, due); addFor = null },
            onDismiss = { addFor = null },
        )
    }
    if (showProfile) ProfileSheet(vm) { showProfile = false }
}

// ------------------------------------------------------------------ top bar

@Composable
private fun TopBar(title: String, vm: MatrixViewModel, onProfile: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val days = vm.streak.shown()
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More options") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("🔥 $days ${if (days == 1) "Day" else "Days"} streak") },
                    onClick = { menu = false },
                    enabled = false,
                )
                DropdownMenuItem(
                    text = { Text("Profile & settings") },
                    leadingIcon = { Avatar(vm.profile, 24.dp) },
                    onClick = { menu = false; onProfile() },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ FAB + bottom bar

@Composable
private fun Fab(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(62.dp).shadow(8.dp, CircleShape).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Default.Add, "Add task", tint = Color.White, modifier = Modifier.size(30.dp)) }
}

@Composable
private fun BottomBar(current: NavTab, onSelect: (NavTab) -> Unit, onMore: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    val active = MaterialTheme.colorScheme.primary
    Surface(Modifier.fillMaxWidth(), color = surface, shadowElevation = 8.dp) {
        Row(Modifier.navigationBarsPadding().padding(vertical = 12.dp)) {
            NavTab.entries.forEach { t ->
                Box(
                    Modifier.weight(1f).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onSelect(t) },
                    contentAlignment = Alignment.Center,
                ) { NavIcon(t.ordinal, if (current == t) active else IdleIcon, surface) }
            }
            Box(
                Modifier.weight(1f).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onMore() },
                contentAlignment = Alignment.Center,
            ) { NavIcon(4, IdleIcon, surface) }
        }
    }
}

// =====================================================================================
// Matrix: 2x2 grid with long-press drag & drop (cross-quadrant move + reorder)
// =====================================================================================

private class DragHandlers(
    val onStart: (Task, Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onEnd: (commit: Boolean) -> Unit,
)

@Composable
private fun MatrixGrid(vm: MatrixViewModel, onAdd: (Quadrant) -> Unit) {
    // Layout bounds (in root coordinates) used to resolve where a drag is dropped.
    val quadBounds = remember { HashMap<Quadrant, Rect>() }
    val cardBounds = remember { HashMap<Long, Rect>() }
    var gridRoot by remember { mutableStateOf(Offset.Zero) }
    var dragTask by remember { mutableStateOf<Task?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    fun quadrantAt(p: Offset): Quadrant? = quadBounds.entries.firstOrNull { it.value.contains(p) }?.key
    val hover: Quadrant? = if (dragTask == null) null else quadrantAt(dragPos)

    val handlers = remember {
        DragHandlers(
            onStart = { task, pos -> dragTask = task; dragPos = pos },
            onDrag = { delta -> dragPos += delta },
            onEnd = { commit ->
                val dragged = dragTask
                val target = quadrantAt(dragPos)
                if (commit && dragged != null && target != null) {
                    // insertion index = number of peers whose centre lies above the drop point
                    val index = vm.tasks
                        .filter { it.quadrant == target && it.id != dragged.id }
                        .count { (cardBounds[it.id]?.center?.y ?: Float.MAX_VALUE) < dragPos.y }
                    vm.moveTo(dragged.id, target, index)
                }
                dragTask = null
            },
        )
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp).onGloballyPositioned { gridRoot = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..1) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0..1) {
                        val q = Quadrant.entries[row * 2 + col]
                        QuadrantPanel(
                            q = q,
                            tasks = vm.tasks.filter { it.quadrant == q },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .onGloballyPositioned { quadBounds[q] = it.boundsInRoot() },
                            highlight = hover == q,
                            onAdd = onAdd,
                        ) { t ->
                            TaskCard(
                                t,
                                onToggle = { vm.toggle(t.id) },
                                onDelete = { vm.remove(t.id) },
                                modifier = Modifier
                                    .alpha(if (dragTask?.id == t.id) 0.25f else 1f)
                                    .onGloballyPositioned { cardBounds[t.id] = it.boundsInRoot() },
                                drag = handlers,
                            )
                        }
                    }
                }
            }
        }

        // Lifted copy of the card under the finger: elevated and slightly scaled up.
        dragTask?.let { t ->
            DragGhost(
                t,
                Modifier.offset {
                    IntOffset(
                        (dragPos.x - gridRoot.x).roundToInt() - 105.dp.roundToPx(),
                        (dragPos.y - gridRoot.y).roundToInt() - 30.dp.roundToPx(),
                    )
                },
            )
        }
    }
}

@Composable
private fun DragGhost(t: Task, modifier: Modifier) {
    var lifted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { lifted = true }
    val scale by animateFloatAsState(if (lifted) 1.06f else 1f, label = "lift")
    Surface(
        modifier.width(210.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 16.dp,
        border = BorderStroke(2.dp, t.quadrant.accent),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(t.text, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
            t.dueDate?.let { Text(formatDue(it), color = DueColor, fontSize = 12.sp) }
        }
    }
}

// ------------------------------------------------------------------ quadrant card

@Composable
private fun QuadrantPanel(
    q: Quadrant,
    tasks: List<Task>,
    modifier: Modifier,
    highlight: Boolean,
    onAdd: (Quadrant) -> Unit,
    card: @Composable (Task) -> Unit,
) {
    Surface(
        modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        // drop-target highlight while a card is dragged over this quadrant
        border = if (highlight) BorderStroke(2.5.dp, q.accent) else BorderStroke(1.dp, q.accent.copy(alpha = 0.0f)),
    ) {
        Column(Modifier.fillMaxSize().clickable { onAdd(q) }.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RomanBadge(q, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(q.title, color = q.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tasks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Tasks", color = muted(), fontSize = 14.sp)
                    }
                } else {
                    // scrolls when tasks overflow the card
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        tasks.forEach { t -> key(t.id) { card(t) } }
                        Spacer(Modifier.height(72.dp)) // keeps the last task clear of the FAB
                    }
                }
            }
        }
    }
}

// =====================================================================================
// Other tabs (Tasks / Recent / Calendar): flat lists over the same data
// =====================================================================================

@Composable
private fun TaskListScreen(tab: NavTab, vm: MatrixViewModel) {
    val list = when (tab) {
        NavTab.Recent -> vm.tasks.sortedByDescending { it.createdAt }
        NavTab.Calendar -> vm.tasks.sortedWith(compareBy<Task> { it.dueDate ?: Long.MAX_VALUE })
        else -> vm.tasks.sortedBy { it.done }
    }
    Surface(
        Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No Tasks", color = muted()) }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
                list.forEach { t ->
                    key(t.id) { TaskCard(t, onToggle = { vm.toggle(t.id) }, onDelete = { vm.remove(t.id) }, showBadge = true) }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// =====================================================================================
// Task row: checkbox, title, due date underneath. Long-press = drag. Tap = delete prompt.
// =====================================================================================

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    drag: DragHandlers? = null,
    showBadge: Boolean = false,
) {
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    var confirmDelete by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val dragModifier = if (drag == null) Modifier else Modifier.pointerInput(task.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = { o ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // pick-up feedback
                drag.onStart(task, rootPos + o)
            },
            onDrag = { change, amount -> change.consume(); drag.onDrag(amount) },
            onDragEnd = { drag.onEnd(true) },
            onDragCancel = { drag.onEnd(false) },
        )
    }

    Row(
        modifier.fillMaxWidth()
            .onGloballyPositioned { rootPos = it.positionInRoot() }
            .clip(RoundedCornerShape(10.dp))
            // onLongPress = {} stops a long-press release from counting as a tap
            .pointerInput(Unit) { detectTapGestures(onLongPress = {}, onTap = { confirmDelete = true }) }
            .then(dragModifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TaskCheck(task.done, task.quadrant.accent, onToggle)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f).padding(top = 5.dp)) {
            Text(
                task.text,
                fontSize = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) muted().copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            )
            task.dueDate?.let { Text(formatDue(it), color = DueColor, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp)) }
        }
        if (showBadge) RomanBadge(task.quadrant, 20.dp, Modifier.padding(top = 6.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text(task.text) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = DueColor) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Compact rounded checkbox in the quadrant's colour (32dp touch target, 22dp visual). */
@Composable
private fun TaskCheck(checked: Boolean, color: Color, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(Modifier.size(32.dp).clip(CircleShape).clickable { onToggle() }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(22.dp).clip(shape)
                .background(if (checked) color else Color.Transparent)
                .border(2.dp, if (checked) color else muted().copy(alpha = 0.6f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(Modifier.size(14.dp)) {
                    val w = this.size.width
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.1f, w * 0.55f); lineTo(w * 0.4f, w * 0.85f); lineTo(w * 0.92f, w * 0.18f)
                        },
                        Color.White,
                        style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
