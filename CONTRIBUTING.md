# Contributing to MyList

Thanks for your interest in improving MyList! Contributions of all sizes are welcome.

## Getting started

1. Fork and clone the repository.
2. Open it in Android Studio (Hedgehog or newer), or build from the command line.
3. Make sure a debug build passes before you start:
   ```bash
   ./gradlew assembleDebug
   ```

## Making changes

- Create a branch off `main`: `git checkout -b my-feature`.
- Keep changes focused — one feature or fix per pull request.
- Match the existing code style (Kotlin official style, 4-space indent). Compose
  UI lives in `app/src/main/java/com/example/androidkotlinapp/ui/`, and the data
  model and tree operations live in `data/` and `NotesViewModel.kt`.
- Build and run the app on an emulator or device to confirm your change works.

## Submitting a pull request

1. Push your branch and open a PR against `main`.
2. Describe what the change does and why; screenshots or screen recordings are
   very helpful for UI changes.
3. The CI workflow builds the app and runs unit tests on every PR — make sure it
   stays green.

## Reporting bugs and ideas

Open an issue with clear steps to reproduce (for bugs) or a short description of
the use case (for feature requests).
