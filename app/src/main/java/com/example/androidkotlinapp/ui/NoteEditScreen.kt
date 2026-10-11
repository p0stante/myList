package com.example.androidkotlinapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidkotlinapp.NotesViewModel
import com.example.androidkotlinapp.addChildToItem
import com.example.androidkotlinapp.addSiblingAfterItem
import com.example.androidkotlinapp.addSiblingsAfterItem
import com.example.androidkotlinapp.data.ListItem
import com.example.androidkotlinapp.data.Note
import com.example.androidkotlinapp.data.NoteColor
import com.example.androidkotlinapp.deleteItemFromTree
import com.example.androidkotlinapp.MAX_DEPTH
import com.example.androidkotlinapp.anyCollapsible
import com.example.androidkotlinapp.anyExpanded
import com.example.androidkotlinapp.indentItemInTree
import com.example.androidkotlinapp.moveChildInParent
import com.example.androidkotlinapp.outdentItemInTree
import com.example.androidkotlinapp.setAllCollapsed
import com.example.androidkotlinapp.setAllCompleted
import com.example.androidkotlinapp.shareNote
import com.example.androidkotlinapp.subtreeHeight
import com.example.androidkotlinapp.updateItemInTree
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private data class FlatItem(
    val item: ListItem,
    val depth: Int,
    val parentId: String?,
    val parentShowsNumbers: Boolean,
    val numberPrefix: String
)

private val numberedMarkerRegex = Regex("^\\s*\\d+[.)]\\s*")
private val bulletMarkerRegex = Regex("^\\s*[-*•●▪‣]\\s*")

/** Strips a leading "1.", "1)" or bullet marker from a pasted line. Returns the cleaned text and whether it was numbered. */
private fun parseListLine(line: String): Pair<String, Boolean> {
    val trimmed = line.trim()
    numberedMarkerRegex.find(trimmed)?.let {
        return trimmed.substring(it.value.length) to true
    }
    bulletMarkerRegex.find(trimmed)?.let {
        return trimmed.substring(it.value.length) to false
    }
    return trimmed to false
}

private fun flattenTree(
    items: List<ListItem>,
    parentShowsNumbers: Boolean,
    depth: Int = 0,
    parentId: String? = null,
    parentPath: List<Int> = emptyList()
): List<FlatItem> {
    val result = mutableListOf<FlatItem>()
    items.forEachIndexed { index, item ->
        val path = if (parentShowsNumbers) parentPath + (index + 1) else parentPath
        val prefix = if (parentShowsNumbers) path.joinToString(".") + "." else ""
        result.add(FlatItem(item, depth, parentId, parentShowsNumbers, prefix))
        if (!item.isCollapsed) {
            result.addAll(flattenTree(item.children, false, depth + 1, item.id, path))
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: String,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var note by remember(noteId) {
        mutableStateOf(viewModel.getNote(noteId) ?: Note(id = noteId))
    }
    var pendingFocusItemId by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }

    fun save() = viewModel.updateNote(note)

    fun updateItem(updated: ListItem) {
        note = note.copy(items = updateItemInTree(note.items, updated))
        save()
    }

    fun deleteItem(id: String) {
        if (note.items.size == 1 && note.items.first().id == id && note.items.first().children.isEmpty()) return
        note = note.copy(items = deleteItemFromTree(note.items, id))
        save()
    }

    fun addChild(parentId: String) {
        val newItem = ListItem()
        note = note.copy(items = addChildToItem(note.items, parentId, newItem))
        pendingFocusItemId = newItem.id
        save()
    }

    fun addSibling(afterId: String) {
        val newItem = ListItem()
        note = note.copy(items = addSiblingAfterItem(note.items, afterId, newItem))
        pendingFocusItemId = newItem.id
        save()
    }

    fun indentItem(id: String) {
        note = note.copy(items = indentItemInTree(note.items, id))
        pendingFocusItemId = id
        save()
    }

    fun outdentItem(id: String) {
        note = note.copy(items = outdentItemInTree(note.items, id))
        pendingFocusItemId = id
        save()
    }

    fun addTopLevelItem() {
        val newItem = ListItem()
        note = note.copy(items = note.items + newItem)
        pendingFocusItemId = newItem.id
        save()
    }

    fun addSiblingsWithText(afterId: String, texts: List<String>) {
        val newItems = texts.map { ListItem(text = it) }
        note = note.copy(items = addSiblingsAfterItem(note.items, afterId, newItems))
        pendingFocusItemId = newItems.last().id
        save()
    }

    fun handleItemTextChange(item: ListItem, newText: String) {
        if (!newText.contains('\n')) {
            updateItem(item.copy(text = newText))
            return
        }
        val lines = newText.split('\n')
        if (lines.size == 2 && lines[1].isEmpty()) {
            // Single Enter press at the end of the line — plain add-sibling behavior.
            updateItem(item.copy(text = lines[0]))
            addSibling(item.id)
            return
        }
        // Multi-line paste — turn it into one list item per non-empty line.
        val parsedLines = lines.map { parseListLine(it) }
        updateItem(item.copy(text = parsedLines.first().first))
        val restTexts = parsedLines.drop(1).map { it.first }.filter { it.isNotBlank() }
        if (restTexts.isNotEmpty()) {
            addSiblingsWithText(item.id, restTexts)
            if (!note.showNumbers && parsedLines.count { it.second } >= 2) {
                note = note.copy(showNumbers = true)
                save()
            }
        } else {
            addSibling(item.id)
        }
    }

    val flatItems by remember { derivedStateOf { flattenTree(note.items, note.showNumbers) } }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromId = from.key as? String ?: return@rememberReorderableLazyListState
        val toId = to.key as? String ?: return@rememberReorderableLazyListState
        val fromFlat = flatItems.find { it.item.id == fromId } ?: return@rememberReorderableLazyListState
        val toFlat = flatItems.find { it.item.id == toId } ?: return@rememberReorderableLazyListState
        if (fromFlat.parentId == toFlat.parentId) {
            note = note.copy(items = moveChildInParent(note.items, fromFlat.parentId, fromId, toId))
            save()
        }
    }

    val bgColor = Color(if (isDark) note.color.darkValue else note.color.lightValue)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { save(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {},
                actions = {
                    val allCompleted = flatItems.isNotEmpty() && flatItems.all { it.item.isCompleted }
                    IconButton(onClick = {
                        note = note.copy(items = setAllCompleted(note.items, !allCompleted))
                        save()
                    }) {
                        Text(
                            text = if (allCompleted) "☑" else "☐",
                            fontSize = 22.sp,
                            color = if (allCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { note = note.copy(showNumbers = !note.showNumbers); save() }) {
                        Text(
                            text = if (note.showNumbers) "•" else "1.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (anyCollapsible(note.items)) {
                        val expanded = anyExpanded(note.items)
                        IconButton(onClick = {
                            note = note.copy(items = setAllCollapsed(note.items, expanded))
                            save()
                        }) {
                            Text(
                                text = if (expanded) "⊟" else "⊞",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(note.color.lightValue), CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться")
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("text") },
                                onClick = {
                                    showShareMenu = false
                                    shareNote(context, note, asJson = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("json") },
                                onClick = {
                                    showShareMenu = false
                                    shareNote(context, note, asJson = true)
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.deleteNote(noteId); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить заметку")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            if (showColorPicker) {
                ColorPickerBar(
                    selected = note.color,
                    onSelect = { note = note.copy(color = it); save() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = note.title,
                    onValueChange = { note = note.copy(title = it); save() },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    decorationBox = { inner ->
                        Box {
                            if (note.title.isEmpty()) {
                                Text(
                                    "Заголовок",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            inner()
                        }
                    }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(flatItems, key = { it.item.id }) { flatItem ->
                val siblings = flatItems.filter { it.parentId == flatItem.parentId }
                val isFirstSibling = siblings.firstOrNull()?.item?.id == flatItem.item.id
                val canIndent = !isFirstSibling &&
                    flatItem.depth + 1 + subtreeHeight(flatItem.item) <= MAX_DEPTH
                val canOutdent = flatItem.depth > 0
                ReorderableItem(reorderableState, key = flatItem.item.id) { isDragging ->
                    FlatItemRow(
                        flatItem = flatItem,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.draggableHandle(),
                        shouldFocus = pendingFocusItemId == flatItem.item.id,
                        onFocused = { pendingFocusItemId = null },
                        onUpdate = { updateItem(it) },
                        onTextChange = { newText -> handleItemTextChange(flatItem.item, newText) },
                        onDelete = { deleteItem(flatItem.item.id) },
                        onAddChild = { addChild(flatItem.item.id) },
                        onAddSibling = { addSibling(flatItem.item.id) },
                        canIndent = canIndent,
                        canOutdent = canOutdent,
                        onIndent = { indentItem(flatItem.item.id) },
                        onOutdent = { outdentItem(flatItem.item.id) },
                        onToggleCollapse = {
                            updateItem(flatItem.item.copy(isCollapsed = !flatItem.item.isCollapsed))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addTopLevelItem() }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Добавить пункт",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 17.sp
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun FlatItemRow(
    flatItem: FlatItem,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    shouldFocus: Boolean,
    onFocused: () -> Unit,
    onUpdate: (ListItem) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit,
    canIndent: Boolean,
    canOutdent: Boolean,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onToggleCollapse: () -> Unit
) {
    val item = flatItem.item
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            focusRequester.requestFocus()
            onFocused()
        }
    }

    val completedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val textColor = if (item.isCompleted) completedColor else MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val checkboxShape = RoundedCornerShape(4.dp)

    val elevation = if (isDragging) 4.dp else 0.dp
    val guideColor = outlineColor.copy(alpha = 0.35f)
    val handleWidth = 20.dp
    val indentStep = 20.dp

    Surface(shadowElevation = elevation, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // One vertical guide line per nesting level, spanning the row
                // height so lines connect between consecutive rows.
                .drawBehind {
                    for (level in 0 until flatItem.depth) {
                        val x = (handleWidth + indentStep * level + indentStep / 2).toPx()
                        drawLine(
                            color = guideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Drag handle
            Icon(
                Icons.Default.Menu,
                contentDescription = "Переместить",
                modifier = dragHandleModifier
                    .padding(top = 3.dp)
                    .size(handleWidth),
                tint = outlineColor.copy(alpha = 0.4f)
            )

            // Depth indentation
            if (flatItem.depth > 0) {
                Spacer(modifier = Modifier.width(indentStep * flatItem.depth))
            }

            // Collapse / expand toggle — only for items with children.
            // A fixed-width slot keeps checkboxes aligned across all rows.
            val hasChildren = item.children.isNotEmpty()
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .then(if (hasChildren) Modifier.clickable { onToggleCollapse() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (hasChildren) {
                    Icon(
                        if (item.isCollapsed) Icons.Default.KeyboardArrowRight
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (item.isCollapsed) "Развернуть" else "Свернуть",
                        modifier = Modifier.size(20.dp),
                        tint = outlineColor
                    )
                }
            }

            // Checkbox — LEFT side, before text
            Box(
                modifier = Modifier
                    .padding(top = 3.dp, end = 10.dp)
                    .size(22.dp)
                    .clip(checkboxShape)
                    .border(
                        2.dp,
                        if (item.isCompleted) primaryColor else outlineColor,
                        checkboxShape
                    )
                    .background(
                        if (item.isCompleted) primaryColor else Color.Transparent,
                        checkboxShape
                    )
                    .clickable { onUpdate(item.copy(isCompleted = !item.isCompleted)) },
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Number prefix (top-level only)
            if (flatItem.numberPrefix.isNotEmpty()) {
                Text(
                    text = flatItem.numberPrefix,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 32.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Text field — fills remaining space
            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = textColor
                ),
                cursorBrush = SolidColor(primaryColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onAddSibling() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (item.text.isEmpty() && !isFocused) {
                            Text(
                                "Пункт",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontSize = 18.sp
                            )
                        }
                        inner()
                    }
                }
            )

            // Action buttons on focus
            if (isFocused) {
                if (canOutdent) {
                    IconButton(
                        onClick = onOutdent,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "На уровень выше",
                            modifier = Modifier.size(22.dp),
                            tint = outlineColor
                        )
                    }
                }
                if (canIndent) {
                    IconButton(
                        onClick = onIndent,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "На уровень ниже",
                            modifier = Modifier.size(22.dp),
                            tint = outlineColor
                        )
                    }
                }
                if (flatItem.depth < MAX_DEPTH) {
                    IconButton(
                        onClick = onAddChild,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Добавить подпункт",
                            modifier = Modifier.size(20.dp),
                            tint = outlineColor
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(18.dp),
                        tint = outlineColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerBar(selected: NoteColor, onSelect: (NoteColor) -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteColor.entries.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(color.lightValue), CircleShape)
                        .border(
                            width = if (color == selected) 3.dp else 1.dp,
                            color = if (color == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (color == selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (color == NoteColor.WHITE) Color.Black
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
