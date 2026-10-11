# MyList

A Google Keep–style checklist app for Android, built with Kotlin and Jetpack Compose.
Make nested to-do lists, number or bullet them, check items off in place, colour-code
notes, and share any list as formatted text or JSON.

## Features

- **Nested checklists** — up to 5 levels deep, with a recursive tree model.
- **Indent / outdent** — promote or demote any item to move it in or out of a sublist.
- **Collapsible sublists** — fold any branch with the chevron, or collapse/expand all
  at once from the top bar. Visual guide lines show the nesting level.
- **Numbering or bullets** — toggle per note; nested numbering like `1.`, `2.1.` …
- **Check off in place** — completed items stay put with a strikethrough.
- **8 note colours**, with light and dark variants.
- **Drag to reorder** items within a level.
- **Local persistence** — every change is saved to disk (JSON), so your lists survive
  restarts without any account or network.
- **Share** a list as human-readable **text** or as **JSON** via the Android share sheet.
- **Light & dark theme**, edge-to-edge, with Material 3 dynamic colour on Android 12+.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- `ViewModel` + `mutableStateListOf` for state
- `kotlinx.serialization` for JSON persistence and export
- [`reorderable`](https://github.com/Calvin-LL/Reorderable) for drag-and-drop
- Min SDK 24, target/compile SDK 35

## Project layout

```
app/src/main/java/com/example/androidkotlinapp/
├── MainActivity.kt            # Activity + screen navigation
├── NotesViewModel.kt          # State, persistence, and tree utilities
├── Sharing.kt                 # Export a note as text / JSON + share intent
├── data/
│   ├── Note.kt                # Note, ListItem, NoteColor models (@Serializable)
│   └── NotesRepository.kt     # File-backed JSON persistence
└── ui/
    ├── NotesScreen.kt         # Grid of note cards
    ├── NoteEditScreen.kt      # The list editor
    └── theme/                 # Colours, typography, Material 3 theme
```

## Building

Open the project in Android Studio (Hedgehog or newer) and run the `app`
configuration, or from the command line:

```bash
./gradlew assembleDebug      # build a debug APK
./gradlew installDebug       # build and install on a connected device/emulator
```

Requires JDK 17 and the Android SDK (SDK 35).

## License

[MIT](LICENSE) © 2026 Alexandra Toropova
