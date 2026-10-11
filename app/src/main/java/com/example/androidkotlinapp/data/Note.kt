package com.example.androidkotlinapp.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ListItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isCompleted: Boolean = false,
    val showNumbers: Boolean = false,
    val isCollapsed: Boolean = false,
    val children: List<ListItem> = emptyList()
)

@Serializable
enum class NoteColor(val lightValue: Long, val darkValue: Long) {
    WHITE(0xFFFFFFFF, 0xFF1C1C1E),
    YELLOW(0xFFFFF8E1, 0xFF3D3000),
    GREEN(0xFFDCEDC8, 0xFF1A3520),
    TEAL(0xFFB2EBF2, 0xFF00363D),
    BLUE(0xFFBBDEFB, 0xFF00264A),
    PURPLE(0xFFE1BEE7, 0xFF33154A),
    PINK(0xFFF8BBD0, 0xFF4A1829),
    ORANGE(0xFFFFE0B2, 0xFF4A2600);

    // backward compat
    val value: Long get() = lightValue
}

@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val items: List<ListItem> = emptyList(),
    val showNumbers: Boolean = false,
    val color: NoteColor = NoteColor.WHITE
)
