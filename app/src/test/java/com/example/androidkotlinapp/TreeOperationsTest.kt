package com.example.androidkotlinapp

import com.example.androidkotlinapp.data.ListItem
import com.example.androidkotlinapp.data.Note
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure tree utilities and note export helpers. */
class TreeOperationsTest {

    private fun item(id: String, text: String = id, children: List<ListItem> = emptyList()) =
        ListItem(id = id, text = text, children = children)

    private fun List<ListItem>.find(id: String): ListItem? {
        for (i in this) {
            if (i.id == id) return i
            i.children.find(id)?.let { return it }
        }
        return null
    }

    // --- subtreeHeight ---

    @Test
    fun subtreeHeight_leafIsZero() {
        assertEquals(0, subtreeHeight(item("a")))
    }

    @Test
    fun subtreeHeight_countsDeepestBranch() {
        val tree = item("a", children = listOf(item("b", children = listOf(item("c")))))
        assertEquals(2, subtreeHeight(tree))
    }

    // --- indentItemInTree ---

    @Test
    fun indent_nestsUnderPrecedingSibling() {
        val items = listOf(item("a"), item("b"))
        val result = indentItemInTree(items, "b")
        assertEquals(1, result.size)
        assertEquals("a", result[0].id)
        assertEquals(listOf("b"), result[0].children.map { it.id })
    }

    @Test
    fun indent_firstSiblingIsNoOp() {
        val items = listOf(item("a"), item("b"))
        assertEquals(items, indentItemInTree(items, "a"))
    }

    @Test
    fun indent_carriesChildrenAlong() {
        val items = listOf(item("a"), item("b", children = listOf(item("b1"))))
        val result = indentItemInTree(items, "b")
        val b = result.find("b")!!
        assertEquals(listOf("b1"), b.children.map { it.id })
    }

    @Test
    fun indent_refusedWhenItWouldExceedMaxDepth() {
        // B has a subtree 4 tall (deepest child already at depth 4 when B is top-level).
        var deep: ListItem = item("d4")
        for (n in 3 downTo 1) deep = item("d$n", children = listOf(deep))
        val b = item("b", children = listOf(deep)) // subtreeHeight(b) == MAX_DEPTH
        val items = listOf(item("a"), b)
        // Indenting B to depth 1 would push its deepest descendant to depth 5 > MAX_DEPTH.
        assertEquals(items, indentItemInTree(items, "b"))
    }

    // --- outdentItemInTree ---

    @Test
    fun outdent_promotesToSiblingAfterParent() {
        val items = listOf(item("a", children = listOf(item("c"))))
        val result = outdentItemInTree(items, "c")
        assertEquals(listOf("a", "c"), result.map { it.id })
        assertTrue(result.find("a")!!.children.isEmpty())
    }

    @Test
    fun outdent_topLevelItemIsNoOp() {
        val items = listOf(item("a"), item("b"))
        assertEquals(items, outdentItemInTree(items, "a"))
    }

    @Test
    fun outdent_keepsItemsOwnChildren() {
        val items = listOf(item("a", children = listOf(item("c", children = listOf(item("c1"))))))
        val result = outdentItemInTree(items, "c")
        assertEquals(listOf("c1"), result.find("c")!!.children.map { it.id })
    }

    @Test
    fun indentThenOutdent_isIdentity() {
        val items = listOf(item("a"), item("b"))
        val round = outdentItemInTree(indentItemInTree(items, "b"), "b")
        assertEquals(listOf("a", "b"), round.map { it.id })
        assertTrue(round.find("a")!!.children.isEmpty())
    }

    // --- basic tree edits ---

    @Test
    fun addChild_appendsToParent() {
        val items = listOf(item("a"))
        val result = addChildToItem(items, "a", item("a1"))
        assertEquals(listOf("a1"), result.find("a")!!.children.map { it.id })
    }

    @Test
    fun addSiblingAfter_insertsRightAfter() {
        val items = listOf(item("a"), item("b"))
        val result = addSiblingAfterItem(items, "a", item("x"))
        assertEquals(listOf("a", "x", "b"), result.map { it.id })
    }

    @Test
    fun delete_removesNestedItem() {
        val items = listOf(item("a", children = listOf(item("a1"), item("a2"))))
        val result = deleteItemFromTree(items, "a1")
        assertEquals(listOf("a2"), result.find("a")!!.children.map { it.id })
    }

    @Test
    fun update_replacesMatchingItemPreservingPosition() {
        val items = listOf(item("a"), item("b"))
        val result = updateItemInTree(items, item("b").copy(text = "changed"))
        assertEquals("changed", result.find("b")!!.text)
    }

    @Test
    fun moveChildInParent_reordersTopLevel() {
        val items = listOf(item("a"), item("b"), item("c"))
        val result = moveChildInParent(items, parentId = null, fromId = "c", toId = "a")
        assertEquals(listOf("c", "a", "b"), result.map { it.id })
    }

    // --- completion & collapse ---

    @Test
    fun setAllCompleted_appliesRecursively() {
        val items = listOf(item("a", children = listOf(item("a1"))))
        val result = setAllCompleted(items, true)
        assertTrue(result.find("a")!!.isCompleted)
        assertTrue(result.find("a1")!!.isCompleted)
    }

    @Test
    fun setAllCollapsed_onlyAffectsItemsWithChildren() {
        val items = listOf(item("a", children = listOf(item("a1"))))
        val result = setAllCollapsed(items, true)
        assertTrue(result.find("a")!!.isCollapsed)
        assertFalse(result.find("a1")!!.isCollapsed) // leaf: no children, unchanged
    }

    @Test
    fun anyExpanded_and_anyCollapsible() {
        val flat = listOf(item("a"), item("b"))
        assertFalse(anyCollapsible(flat))
        assertFalse(anyExpanded(flat))

        val nested = listOf(item("a", children = listOf(item("a1"))))
        assertTrue(anyCollapsible(nested))
        assertTrue(anyExpanded(nested))
        assertFalse(anyExpanded(setAllCollapsed(nested, true)))
    }

    // --- export helpers (Sharing.kt) ---

    @Test
    fun toFormattedText_numbersTopLevelAndBulletsChildren() {
        val note = Note(
            title = "T",
            showNumbers = true,
            items = listOf(
                item("a", text = "a", children = listOf(
                    ListItem(id = "b", text = "b", isCompleted = true)
                )),
                item("c", text = "c")
            )
        )
        val expected = "T\n\n[ ] 1. a\n    [x] • b\n[ ] 2. c"
        assertEquals(expected, note.toFormattedText())
    }

    @Test
    fun toFormattedText_bulletsWhenNumbersOff() {
        val note = Note(items = listOf(item("a", text = "a"), item("b", text = "b")))
        assertEquals("[ ] • a\n[ ] • b", note.toFormattedText())
    }

    @Test
    fun toJson_roundTripsBackToEqualNote() {
        val note = Note(
            title = "Groceries",
            showNumbers = true,
            items = listOf(item("a", text = "Milk", children = listOf(item("a1", text = "2%"))))
        )
        val decoded = Json.decodeFromString(Note.serializer(), note.toJson())
        assertEquals(note, decoded)
    }

    @Test
    fun findMissingItem_returnsNull() {
        assertNull(listOf(item("a")).find("nope"))
    }
}
