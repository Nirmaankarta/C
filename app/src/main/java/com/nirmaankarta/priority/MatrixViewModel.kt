package com.nirmaankarta.priority

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.math.max

class MatrixViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    val tasks = mutableStateListOf<Task>().also { it.addAll(store.loadTasks()) }
    var profile by mutableStateOf(store.loadProfile()); private set
    var streak by mutableStateOf(store.loadStreak()); private set

    // ---- sound ----
    fun play(q: Quadrant, add: Boolean) { if (profile.sfx) Sfx.play(q, add) }
    fun audition(q: Quadrant, add: Boolean) = Sfx.play(q, add) // ignores the SFX switch

    // ---- tasks ----
    fun addTask(text: String, q: Quadrant, due: Long? = null) {
        val t = text.trim()
        if (t.isEmpty()) return
        tasks.add(Task(id = System.nanoTime(), text = t, quadrant = q, dueDate = due))
        saveTasks()
        play(q, true)
        registerActivity()
    }

    fun toggle(id: Long) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i < 0) return
        val t = tasks[i]
        tasks[i] = t.copy(done = !t.done)
        saveTasks()
        if (!t.done) { // just completed
            play(t.quadrant, false)
            registerActivity()
        }
    }

    /**
     * Drag & drop: puts task [id] into quadrant [q] at position [index] among that
     * quadrant's other tasks. Works for both cross-quadrant moves and re-ordering.
     */
    fun moveTo(id: Long, q: Quadrant, index: Int) {
        val from = tasks.indexOfFirst { it.id == id }
        if (from < 0) return
        val old = tasks.removeAt(from)
        val peers = tasks.filter { it.quadrant == q }
        val at = when {
            peers.isEmpty() -> tasks.size
            index >= peers.size -> tasks.indexOf(peers.last()) + 1
            else -> tasks.indexOf(peers[index.coerceAtLeast(0)])
        }
        tasks.add(at, old.copy(quadrant = q))
        saveTasks()
        if (old.quadrant != q) play(q, true)
    }

    fun remove(id: Long) {
        tasks.removeAll { it.id == id }
        saveTasks()
    }

    private fun saveTasks() = store.saveTasks(tasks.toList())

    // ---- streak ----
    private fun registerActivity() {
        val today = LocalDate.now()
        val last = runCatching { LocalDate.parse(streak.last) }.getOrNull()
        val n = when {
            last == null -> 1
            last == today -> max(streak.count, 1)          // same day: keep
            last == today.minusDays(1) -> streak.count + 1 // consecutive day
            else -> 1                                      // gap: reset
        }
        streak = Streak(n, today.toString())
        store.saveStreak(streak)
    }

    // ---- profile ----
    fun updateProfile(p: Profile) {
        profile = p
        store.saveProfile(p)
    }

    fun setCustomAvatar(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val b64 = runCatching {
                val cr = getApplication<Application>().contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                val bmp = cr.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: error("decode failed")
                val side = minOf(bmp.width, bmp.height)
                val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
                val small = Bitmap.createScaledBitmap(square, 256, 256, true)
                val out = ByteArrayOutputStream()
                small.compress(Bitmap.CompressFormat.JPEG, 85, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }.getOrNull()
            if (b64 != null) withContext(Dispatchers.Main) { updateProfile(profile.copy(customAvatar = b64)) }
        }
    }
}
