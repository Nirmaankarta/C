package com.nirmaankarta.priority

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

@Composable
fun MatrixApp(vm: MatrixViewModel) {
    var addFor by remember { mutableStateOf<Quadrant?>(null) }
    var showProfile by remember { mutableStateOf(false) }

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(vm, onProfile = { showProfile = true })
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 600.dp) TabletGrid(vm) { addFor = it }
                else PhoneLayout(vm) { addFor = it }
            }
        }
    }

    addFor?.let { q ->
        AddTaskSheet(
            initial = q,
            onPreview = { vm.play(it, true) },
            onSubmit = { text, quad -> vm.addTask(text, quad); addFor = null },
            onDismiss = { addFor = null },
        )
    }
    if (showProfile) ProfileSheet(vm) { showProfile = false }
}

// ------------------------------------------------------------------ header

@Composable
private fun Header(vm: MatrixViewModel, onProfile: () -> Unit) {
    val days = vm.streak.shown()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Priority", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Eisenhower Matrix", fontSize = 12.sp, color = muted())
        }
        Surface(shape = RoundedCornerShape(50), color = Quadrant.Q3.accent.copy(alpha = 0.16f)) {
            Text(
                "🔥 $days ${if (days == 1) "Day" else "Days"}",
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onProfile)) {
            Avatar(vm.profile, 40.dp)
        }
    }
}

// ------------------------------------------------------------------ phone

@Composable
private fun PhoneLayout(vm: MatrixViewModel, onAdd: (Quadrant) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 = All, 1..4 = Q1..Q4
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabBar(tab) { tab = it }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val shown = if (tab == 0) Quadrant.entries else listOf(Quadrant.entries[tab - 1])
                shown.forEach { q ->
                    QuadrantPanel(
                        q = q,
                        tasks = vm.tasks.filter { it.quadrant == q }.sortedBy { it.done },
                        modifier = if (tab == 0) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().heightIn(min = 420.dp),
                        onAdd = onAdd,
                    ) { t ->
                        TaskCard(
                            t, onToggle = { vm.toggle(t.id) }, onDelete = { vm.remove(t.id) },
                            onMove = { vm.move(t.id, it) },
                        )
                    }
                }
                Spacer(Modifier.height(100.dp)) // room for the FAB
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
                .size(60.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable { onAdd(if (tab == 0) Quadrant.Q1 else Quadrant.entries[tab - 1]) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "Add task", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("All") + Quadrant.entries.map { it.label }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val color = if (i == 0) MaterialTheme.colorScheme.onBackground else Quadrant.entries[i - 1].accent
            val sel = selected == i
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) color.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) color else muted(), fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

// ------------------------------------------------------------------ tablet / desktop-size grid

private class DragHandlers(
    val onStart: (Task, Offset) -> Unit,
    val onDrag: (Offset) -> Unit,
    val onEnd: (Boolean) -> Unit,
)

@Composable
private fun TabletGrid(vm: MatrixViewModel, onAdd: (Quadrant) -> Unit) {
    val bounds = remember { mutableStateMapOf<Quadrant, Rect>() }
    var gridRoot by remember { mutableStateOf(Offset.Zero) }
    var dragTask by remember { mutableStateOf<Task?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val hover: Quadrant? = if (dragTask == null) null else bounds.entries.firstOrNull { it.value.contains(dragPos) }?.key

    val handlers = remember {
        DragHandlers(
            onStart = { task, pos -> dragTask = task; dragPos = pos },
            onDrag = { delta -> dragPos += delta },
            onEnd = { commit ->
                val dragged = dragTask
                if (commit && dragged != null) {
                    val target = bounds.entries.firstOrNull { e -> e.value.contains(dragPos) }?.key
                    if (target != null) vm.move(dragged.id, target)
                }
                dragTask = null
            },
        )
    }

    Box(Modifier.fillMaxSize().padding(12.dp).onGloballyPositioned { gridRoot = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..1) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0..1) {
                        val q = Quadrant.entries[row * 2 + col]
                        QuadrantPanel(
                            q = q,
                            tasks = vm.tasks.filter { it.quadrant == q }.sortedBy { it.done },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .onGloballyPositioned { bounds[q] = it.boundsInRoot() },
                            scrollable = true,
                            highlight = hover == q,
                            onAdd = onAdd,
                        ) { t ->
                            TaskCard(
                                t, onToggle = { vm.toggle(t.id) }, onDelete = { vm.remove(t.id) },
                                modifier = Modifier.alpha(if (dragTask?.id == t.id) 0.3f else 1f),
                                drag = handlers,
                            )
                        }
                    }
                }
            }
        }

        // central quick-add at the crossroads
        Box(
            Modifier.align(Alignment.Center).size(64.dp)
                .border(5.dp, MaterialTheme.colorScheme.background, CircleShape)
                .shadow(6.dp, CircleShape).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable { onAdd(Quadrant.Q1) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, "Add task", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(28.dp))
        }

        // floating copy of the card being dragged
        dragTask?.let { t ->
            Surface(
                Modifier.width(220.dp).offset {
                    IntOffset(
                        (dragPos.x - gridRoot.x).roundToInt() - 110.dp.roundToPx(),
                        (dragPos.y - gridRoot.y).roundToInt() - 28.dp.roundToPx(),
                    )
                },
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 12.dp,
                border = BorderStroke(2.dp, t.quadrant.accent),
            ) {
                Text(t.text, Modifier.padding(14.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ------------------------------------------------------------------ shared pieces

@Composable
private fun QuadrantPanel(
    q: Quadrant,
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    highlight: Boolean = false,
    onAdd: (Quadrant) -> Unit,
    card: @Composable (Task) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val fill = q.accent.copy(alpha = if (isDarkTheme()) 0.10f else 0.05f)
    Column(
        modifier.clip(shape).background(fill)
            .border(if (highlight) 2.dp else 1.dp, q.accent.copy(alpha = if (highlight) 1f else 0.25f), shape)
            .clickable { onAdd(q) } // tap on empty space opens the creator for this quadrant
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(q.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { QuadrantIcon(q, q.accent, 18.dp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(q.title, color = q.accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(q.tagline, color = muted(), fontSize = 12.sp)
            }
            Text("${tasks.count { !it.done }}", color = q.accent, fontWeight = FontWeight.Bold)
        }
        if (tasks.isEmpty()) {
            Text("Tap to add a task", color = muted().copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        }
        tasks.forEach { t -> key(t.id) { card(t) } }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: ((Quadrant) -> Unit)? = null, // phone quick-mover
    drag: DragHandlers? = null,           // tablet drag & drop
) {
    var root by remember { mutableStateOf(Offset.Zero) }
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val accent = task.quadrant.accent

    val dragModifier = if (drag == null) Modifier else Modifier.pointerInput(task.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = { o ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                drag.onStart(task, root + o)
            },
            onDrag = { change, amount -> change.consume(); drag.onDrag(amount) },
            onDragEnd = { drag.onEnd(true) },
            onDragCancel = { drag.onEnd(false) },
        )
    }

    Surface(
        modifier.fillMaxWidth().onGloballyPositioned { root = it.positionInRoot() }
            // consume taps so they don't fall through to the quadrant's "add" handler
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
            .then(dragModifier),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.done, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = accent.copy(alpha = 0.7f)),
            )
            Text(
                task.text,
                Modifier.weight(1f).padding(vertical = 8.dp),
                fontSize = 15.sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) muted().copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            )
            if (onMove != null) {
                Box {
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { menu = true }
                            .background(accent.copy(alpha = 0.12f)).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(task.quadrant.label, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, "Move to another quadrant", tint = accent, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Quadrant.entries.forEach { q ->
                            DropdownMenuItem(
                                text = { Text("${q.label} · ${q.title}") },
                                leadingIcon = { QuadrantIcon(q, q.accent, 18.dp) },
                                onClick = { menu = false; onMove(q) },
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete task", tint = muted(), modifier = Modifier.size(20.dp))
            }
        }
    }
}
