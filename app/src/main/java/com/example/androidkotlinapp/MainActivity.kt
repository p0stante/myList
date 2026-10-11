package com.example.androidkotlinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.androidkotlinapp.ui.NoteEditScreen
import com.example.androidkotlinapp.ui.NotesScreen
import com.example.androidkotlinapp.ui.theme.AndroidKotlinAppTheme

sealed class Screen {
    object NotesList : Screen()
    data class NoteEdit(val noteId: String) : Screen()
}

class MainActivity : ComponentActivity() {
    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidKotlinAppTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.NotesList) }

                when (val s = screen) {
                    is Screen.NotesList -> NotesScreen(
                        viewModel = viewModel,
                        onNoteClick = { id -> screen = Screen.NoteEdit(id) },
                        onNewNote = { id -> screen = Screen.NoteEdit(id) }
                    )
                    is Screen.NoteEdit -> {
                        BackHandler { screen = Screen.NotesList }
                        NoteEditScreen(
                            noteId = s.noteId,
                            viewModel = viewModel,
                            onBack = { screen = Screen.NotesList }
                        )
                    }
                }
            }
        }
    }
}
