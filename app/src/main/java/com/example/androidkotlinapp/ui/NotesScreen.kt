package com.example.androidkotlinapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidkotlinapp.NotesViewModel
import com.example.androidkotlinapp.data.ListItem
import com.example.androidkotlinapp.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun flattenForPreview(
    items: List<ListItem>,
    depth: Int = 0
): List<Pair<ListItem, Int>> {
    val result = mutableListOf<Pair<ListItem, Int>>()
    items.forEach { item ->
        result.add(item to depth)
        result.addAll(flattenForPreview(item.children, depth + 1))
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String) -> Unit,
    onNewNote: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {})
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val id = viewModel.addNote()
                onNewNote(id)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Новый список", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        if (viewModel.notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Нажмите + чтобы создать новый список",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(
                    items = viewModel.notes,
                    key = { it.id }
                ) { note ->
                    NoteCard(note = note, onClick = { onNoteClick(note.id) })
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val bgColor = Color(if (isDark) note.color.darkValue else note.color.lightValue)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val dateText = remember(note.createdAt) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(note.createdAt))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            val allItems = flattenForPreview(note.items)
            val previewItems = allItems.filter { it.first.text.isNotEmpty() }.take(7)

            previewItems.forEach { (item, depth) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (depth > 0) Spacer(modifier = Modifier.width((depth * 8).dp))
                    Text(
                        text = if (item.isCompleted) "☑ " else "☐ ",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.text,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough
                                         else TextDecoration.None,
                        color = if (item.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val hiddenCount = allItems.count { it.first.text.isNotEmpty() } - previewItems.size
            if (hiddenCount > 0) {
                Text(
                    text = "+$hiddenCount ещё",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
