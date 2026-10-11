package com.example.androidkotlinapp

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidkotlinapp.data.ListItem
import com.example.androidkotlinapp.data.Note
import com.example.androidkotlinapp.data.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = NotesRepository(app)
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes

    private val saveMutex = Mutex()

    init {
        _notes.addAll(repository.load())
    }

    /** Writes a snapshot of the current notes to disk off the main thread. */
    private fun persist() {
        val snapshot = _notes.toList()
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock { repository.save(snapshot) }
        }
    }

    fun addNote(): String {
        val note = Note(items = listOf(ListItem()))
        _notes.add(0, note)
        persist()
        return note.id
    }

    fun getNote(id: String): Note? = _notes.find { it.id == id }

    fun updateNote(note: Note) {
        val i = _notes.indexOfFirst { it.id == note.id }
        if (i >= 0) {
            _notes[i] = note
            persist()
        }
    }

    fun deleteNote(id: String) {
        if (_notes.removeIf { it.id == id }) persist()
    }
}

// --- Tree utility functions ---

fun updateItemInTree(items: List<ListItem>, updated: ListItem): List<ListItem> =
    items.map { item ->
        if (item.id == updated.id) updated
        else item.copy(children = updateItemInTree(item.children, updated))
    }

fun deleteItemFromTree(items: List<ListItem>, id: String): List<ListItem> =
    items
        .filter { it.id != id }
        .map { it.copy(children = deleteItemFromTree(it.children, id)) }

fun addChildToItem(items: List<ListItem>, parentId: String, newChild: ListItem): List<ListItem> =
    items.map { item ->
        if (item.id == parentId) item.copy(children = item.children + newChild)
        else item.copy(children = addChildToItem(item.children, parentId, newChild))
    }

fun addSiblingAfterItem(items: List<ListItem>, afterId: String, newItem: ListItem): List<ListItem> {
    val idx = items.indexOfFirst { it.id == afterId }
    if (idx >= 0) {
        return items.toMutableList().also { it.add(idx + 1, newItem) }
    }
    return items.map { item ->
        item.copy(children = addSiblingAfterItem(item.children, afterId, newItem))
    }
}

fun addSiblingsAfterItem(items: List<ListItem>, afterId: String, newItems: List<ListItem>): List<ListItem> {
    if (newItems.isEmpty()) return items
    val idx = items.indexOfFirst { it.id == afterId }
    if (idx >= 0) {
        return items.toMutableList().also { it.addAll(idx + 1, newItems) }
    }
    return items.map { item ->
        item.copy(children = addSiblingsAfterItem(item.children, afterId, newItems))
    }
}

fun setAllCompleted(items: List<ListItem>, completed: Boolean): List<ListItem> =
    items.map { it.copy(isCompleted = completed, children = setAllCompleted(it.children, completed)) }

/** Collapses or expands every item that has children, at every level. */
fun setAllCollapsed(items: List<ListItem>, collapsed: Boolean): List<ListItem> =
    items.map {
        it.copy(
            isCollapsed = if (it.children.isEmpty()) it.isCollapsed else collapsed,
            children = setAllCollapsed(it.children, collapsed)
        )
    }

/** True if any item anywhere has children (i.e. there is something to collapse). */
fun anyCollapsible(items: List<ListItem>): Boolean =
    items.any { it.children.isNotEmpty() || anyCollapsible(it.children) }

/** True if any item with children is currently expanded. */
fun anyExpanded(items: List<ListItem>): Boolean =
    items.any { (it.children.isNotEmpty() && !it.isCollapsed) || anyExpanded(it.children) }

/** Max nesting depth (0-based): items may live at depth 0..4 (5 levels). */
const val MAX_DEPTH = 4

/** Height of a subtree: a leaf is 0, a node with children is 1 + tallest child. */
fun subtreeHeight(item: ListItem): Int =
    if (item.children.isEmpty()) 0 else 1 + item.children.maxOf { subtreeHeight(it) }

/**
 * Demotes an item to become the last child of its immediately preceding sibling.
 * No-op if the item is the first in its list or if the move would push any
 * descendant past [MAX_DEPTH]. [depth] is the depth of the items in this list.
 */
fun indentItemInTree(items: List<ListItem>, id: String, depth: Int = 0): List<ListItem> {
    val idx = items.indexOfFirst { it.id == id }
    if (idx >= 0) {
        if (idx == 0) return items // nothing to nest under
        val item = items[idx]
        if (depth + 1 + subtreeHeight(item) > MAX_DEPTH) return items
        val prev = items[idx - 1]
        return items.toMutableList().also {
            it[idx - 1] = prev.copy(children = prev.children + item)
            it.removeAt(idx)
        }
    }
    return items.map { it.copy(children = indentItemInTree(it.children, id, depth + 1)) }
}

/**
 * Promotes an item out of its sublist to become a sibling of its parent,
 * inserted immediately after that parent. No-op for top-level items.
 */
fun outdentItemInTree(items: List<ListItem>, id: String): List<ListItem> {
    val result = mutableListOf<ListItem>()
    for (node in items) {
        val childIdx = node.children.indexOfFirst { it.id == id }
        if (childIdx >= 0) {
            val child = node.children[childIdx]
            result.add(node.copy(children = node.children.toMutableList().also { it.removeAt(childIdx) }))
            result.add(child) // becomes a sibling right after its former parent
        } else {
            result.add(node.copy(children = outdentItemInTree(node.children, id)))
        }
    }
    return result
}

fun moveChildInParent(
    items: List<ListItem>,
    parentId: String?,
    fromId: String,
    toId: String
): List<ListItem> {
    if (parentId == null) {
        val m = items.toMutableList()
        val fi = m.indexOfFirst { it.id == fromId }
        val ti = m.indexOfFirst { it.id == toId }
        if (fi < 0 || ti < 0) return items
        m.add(ti, m.removeAt(fi))
        return m
    }
    return items.map { item ->
        if (item.id == parentId) {
            val m = item.children.toMutableList()
            val fi = m.indexOfFirst { it.id == fromId }
            val ti = m.indexOfFirst { it.id == toId }
            if (fi < 0 || ti < 0) item
            else { m.add(ti, m.removeAt(fi)); item.copy(children = m) }
        } else {
            item.copy(children = moveChildInParent(item.children, parentId, fromId, toId))
        }
    }
}
