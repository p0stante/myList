package com.example.androidkotlinapp

import android.content.Context
import android.content.Intent
import com.example.androidkotlinapp.data.ListItem
import com.example.androidkotlinapp.data.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Renders a note as human-readable text: a title, then each item with a
 * checkbox, a number (when numbering is on) or bullet, and depth indentation.
 * Mirrors the on-screen numbering (top level follows [Note.showNumbers];
 * nested items are bulleted).
 */
fun Note.toFormattedText(): String {
    val sb = StringBuilder()
    if (title.isNotBlank()) {
        sb.append(title).append("\n\n")
    }

    fun render(items: List<ListItem>, showNumbers: Boolean, depth: Int, parentPath: List<Int>) {
        items.forEachIndexed { index, item ->
            val path = if (showNumbers) parentPath + (index + 1) else parentPath
            val marker = if (showNumbers) path.joinToString(".") + "." else "•"
            val box = if (item.isCompleted) "[x]" else "[ ]"
            sb.append("    ".repeat(depth))
                .append(box).append(' ')
                .append(marker).append(' ')
                .append(item.text).append('\n')
            render(item.children, showNumbers = false, depth + 1, path)
        }
    }

    render(items, showNumbers, 0, emptyList())
    return sb.toString().trimEnd('\n')
}

/** Serializes a single note to pretty-printed JSON. */
fun Note.toJson(): String = prettyJson.encodeToString(this)

/** Opens the Android share sheet with the note as formatted text or JSON. */
fun shareNote(context: Context, note: Note, asJson: Boolean) {
    val content = if (asJson) note.toJson() else note.toFormattedText()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
        if (note.title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, note.title)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться списком"))
}
