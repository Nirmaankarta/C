package com.nirmaankarta.priority

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** All persistence lives in SharedPreferences (the Android equivalent of localStorage). */
class Store(context: Context) {
    private val sp = context.getSharedPreferences("priority", Context.MODE_PRIVATE)

    fun loadTasks(): List<Task> = runCatching {
        val arr = JSONArray(sp.getString("tasks", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Task(
                id = o.getLong("id"),
                text = o.getString("text"),
                quadrant = Quadrant.valueOf(o.getString("q")),
                done = o.optBoolean("done"),
                createdAt = o.optLong("at"),
                dueDate = if (o.isNull("due")) null else o.getLong("due"),
            )
        }
    }.getOrDefault(emptyList())

    fun saveTasks(list: List<Task>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("text", it.text).put("q", it.quadrant.name)
                    .put("done", it.done).put("at", it.createdAt).put("due", it.dueDate ?: JSONObject.NULL)
            )
        }
        sp.edit().putString("tasks", arr.toString()).apply()
    }

    fun loadProfile() = Profile(
        name = sp.getString("name", "You") ?: "You",
        username = sp.getString("username", "you") ?: "you",
        avatar = sp.getInt("avatar", 0),
        customAvatar = sp.getString("customAvatar", null),
        sfx = sp.getBoolean("sfx", true),
        theme = runCatching { ThemeMode.valueOf(sp.getString("theme", "System")!!) }
            .getOrDefault(ThemeMode.System),
    )

    fun saveProfile(p: Profile) {
        sp.edit()
            .putString("name", p.name).putString("username", p.username)
            .putInt("avatar", p.avatar).putString("customAvatar", p.customAvatar)
            .putBoolean("sfx", p.sfx).putString("theme", p.theme.name)
            .apply()
    }

    fun loadStreak() = Streak(sp.getInt("streak", 0), sp.getString("streakLast", null))

    fun saveStreak(s: Streak) {
        sp.edit().putInt("streak", s.count).putString("streakLast", s.last).apply()
    }
}
