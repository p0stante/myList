package com.example.androidkotlinapp.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-backed JSON persistence for all notes. Stored in the app's private files
 * directory so the data survives process death and app restarts.
 */
class NotesRepository(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): List<Note> =
        try {
            if (!file.exists()) emptyList()
            else file.readText().takeIf { it.isNotBlank() }
                ?.let { json.decodeFromString<List<Note>>(it) }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    /** Atomically replaces the stored notes (write to temp file, then rename). */
    fun save(notes: List<Note>) {
        try {
            val tmp = File(file.parentFile, "notes.json.tmp")
            tmp.writeText(json.encodeToString(notes))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) {
            // Best-effort; a failed write shouldn't crash the app.
        }
    }
}
