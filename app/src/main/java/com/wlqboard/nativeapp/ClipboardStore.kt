package com.wlqboard.nativeapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

/** Local, passive clipboard history; pruning runs only when the visible IME opens the store. */
class ClipboardStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "vozflow_clipboard.db", null, 1) {
    data class Entry(val id: Long, val payload: String, val image: Boolean, val pinned: Boolean, val createdAt: Long)

    private val preferences = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE clipboard_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                payload TEXT NOT NULL UNIQUE,
                is_image INTEGER NOT NULL DEFAULT 0,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX clipboard_items_recent ON clipboard_items(is_pinned DESC, created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun migrateLegacyAndPrune(recentLimit: Int) {
        if (!preferences.getBoolean(MIGRATED_KEY, false)) {
            val history = try { JSONArray(preferences.getString("clipboard_history", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            val pinned = preferences.getStringSet("clipboard_pinned", emptySet()).orEmpty()
            for (index in 0 until history.length()) {
                val payload = history.optString(index).trim()
                if (payload.isNotEmpty()) insert(payload, payload.startsWith(IMAGE_PREFIX), pinned.contains(payload), System.currentTimeMillis())
            }
            preferences.edit().putBoolean(MIGRATED_KEY, true).apply()
        }
        prune(recentLimit)
    }

    @Synchronized
    fun add(payload: String, image: Boolean, recentLimit: Int) {
        val clean = payload.trim()
        if (clean.isEmpty()) return
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            val pinned = queryPinned(clean)
            db.execSQL(
                "INSERT OR REPLACE INTO clipboard_items(payload,is_image,is_pinned,created_at) VALUES(?,?,?,?)",
                arrayOf<Any>(clean, if (image) 1 else 0, if (pinned) 1 else 0, now)
            )
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        prune(recentLimit)
    }

    @Synchronized
    fun entries(): List<Entry> {
        val result = mutableListOf<Entry>()
        readableDatabase.query("clipboard_items", arrayOf("id", "payload", "is_image", "is_pinned", "created_at"), null, null, null, null, "is_pinned DESC, created_at DESC").use { cursor ->
            while (cursor.moveToNext()) result += Entry(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) != 0, cursor.getInt(3) != 0, cursor.getLong(4))
        }
        return result
    }

    @Synchronized
    fun setPinned(id: Long, pinned: Boolean) {
        writableDatabase.execSQL("UPDATE clipboard_items SET is_pinned=? WHERE id=?", arrayOf(if (pinned) 1 else 0, id))
    }

    @Synchronized
    fun updateText(id: Long, text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        writableDatabase.execSQL("UPDATE clipboard_items SET payload=?,is_image=0,created_at=? WHERE id=?", arrayOf<Any>(value, System.currentTimeMillis(), id))
    }

    @Synchronized
    fun remove(id: Long) { writableDatabase.delete("clipboard_items", "id=?", arrayOf(id.toString())) }

    @Synchronized
    fun remove(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        writableDatabase.beginTransaction()
        try {
            ids.forEach { writableDatabase.delete("clipboard_items", "id=?", arrayOf(it.toString())) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    @Synchronized
    fun clear() { writableDatabase.delete("clipboard_items", null, null) }

    @Synchronized
    fun prune(recentLimit: Int) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.delete("clipboard_items", "is_pinned=0 AND created_at<?", arrayOf((now - ONE_HOUR_MS).toString()))
        val keep = recentLimit.coerceIn(1, 200)
        val expired = mutableListOf<Long>()
        db.rawQuery("SELECT id FROM clipboard_items WHERE is_pinned=0 ORDER BY created_at DESC LIMIT -1 OFFSET ?", arrayOf(keep.toString())).use { cursor ->
            while (cursor.moveToNext()) expired += cursor.getLong(0)
        }
        remove(expired)
    }

    @Synchronized
    private fun insert(payload: String, image: Boolean, pinned: Boolean, time: Long) {
        writableDatabase.execSQL(
            "INSERT OR IGNORE INTO clipboard_items(payload,is_image,is_pinned,created_at) VALUES(?,?,?,?)",
            arrayOf<Any>(payload, if (image) 1 else 0, if (pinned) 1 else 0, time)
        )
    }

    private fun queryPinned(payload: String): Boolean = readableDatabase.rawQuery(
        "SELECT is_pinned FROM clipboard_items WHERE payload=? LIMIT 1", arrayOf(payload)
    ).use { it.moveToFirst() && it.getInt(0) != 0 }

    companion object {
        const val IMAGE_PREFIX = "__VOICEFLOW_IMAGE__:"
        private const val MIGRATED_KEY = "clipboard_sqlite_migrated_v1"
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
    }
}
