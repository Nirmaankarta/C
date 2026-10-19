@file:OptIn(ExperimentalMaterial3Api::class)

package com.nirmaankarta.priority

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ add task (bottom sheet)

@Composable
fun AddTaskSheet(
    initial: Quadrant,
    onPreview: (Quadrant) -> Unit,
    onSubmit: (String, Quadrant) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var q by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val submit = { if (text.isNotBlank()) onSubmit(text, q) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("New task", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Quadrant.entries.forEach { opt ->
                    val sel = opt == q
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.weight(1f).clip(shape)
                            .background(opt.accent.copy(alpha = if (sel) 0.2f else 0.07f))
                            .border(if (sel) 2.dp else 1.dp, opt.accent.copy(alpha = if (sel) 1f else 0.3f), shape)
                            .clickable { q = opt; onPreview(opt) } // selecting plays its signature sound
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        QuadrantIcon(opt, opt.accent, 26.dp)
                        Text(opt.label, color = opt.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Button(
                onClick = { submit() },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = q.accent, contentColor = Color.White),
            ) { Text("Add to ${q.title}") }
        }
    }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
}

// ------------------------------------------------------------------ profile & preferences

@Composable
fun ProfileSheet(vm: MatrixViewModel, onDismiss: () -> Unit) {
    val p = vm.profile
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setCustomAvatar(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                .padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p, 56.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(p.name.ifBlank { "You" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("@${p.username}", color = muted())
                }
            }

            Section("Profile")
            OutlinedTextField(
                value = p.name, onValueChange = { vm.updateProfile(p.copy(name = it.take(40))) },
                label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = p.username,
                onValueChange = { v -> vm.updateProfile(p.copy(username = v.filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(30))) },
                label = { Text("Username") }, prefix = { Text("@") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Section("Avatar")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until PRESET_AVATARS) {
                    val sel = p.customAvatar == null && p.avatar == i
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .border(if (sel) 3.dp else 0.dp, if (sel) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable { vm.updateProfile(p.copy(avatar = i, customAvatar = null)) },
                        contentAlignment = Alignment.Center,
                    ) { PresetAvatar(i, 56.dp) }
                }
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Add, "Upload photo", tint = muted()) }
            }
            if (p.customAvatar != null) {
                TextButton(onClick = { vm.updateProfile(p.copy(customAvatar = null)) }) { Text("Remove uploaded photo") }
            }

            Section("Sound & theme")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound effects", Modifier.weight(1f))
                Switch(checked = p.sfx, onCheckedChange = { vm.updateProfile(p.copy(sfx = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(
                        selected = p.theme == m,
                        onClick = { vm.updateProfile(p.copy(theme = m)) },
                        label = { Text(m.name) },
                    )
                }
            }

            Section("Audio test center")
            Quadrant.entries.forEach { q ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuadrantIcon(q, q.accent, 22.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(q.label, Modifier.width(36.dp), color = q.accent, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { vm.audition(q, true) }, Modifier.weight(1f),
                        border = BorderStroke(1.dp, q.accent.copy(alpha = 0.6f)),
                    ) { Text("Add", color = q.accent) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { vm.audition(q, false) }, Modifier.weight(1f),
                        border = BorderStroke(1.dp, q.accent.copy(alpha = 0.6f)),
                    ) { Text("Complete", color = q.accent) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), color = muted(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}
