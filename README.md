# Local Notes

A native Android notes app (Kotlin) with customizable note colors, a built-in
calculator, and plain-text storage you can actually see and back up.

No account, no cloud, no database file you can't read. Every note is a
plain-text file.

## Where your data lives

- **Notes:** `Android/media/notes/*.txt` — one file per note. Each file has a
  tiny plain-text header (color, pinned/archived/deleted flags, timestamps)
  followed by the title and body, so you can open any note in a plain text
  editor and understand it immediately.
- **Settings:** `Android/media/settings.properties` — theme and sort
  preference, mirrored here every time you change them in the app.

The app requests **All files access** (`MANAGE_EXTERNAL_STORAGE`) on first
launch so it can read and write that shared `Android/media` folder rather
than being locked into its own private sandbox.

## Features

- Pin, archive, and soft-delete (trash) notes, with a dedicated trash view
  and "empty trash" action
- Five note colors (Butter, Sky, Sage, Rose, Iris), each with light and dark
  variants
- Full light/dark/system theme support
- Sort by last edited, date created, or title
- Search across title and body
- A genuinely offline calculator with parentheses support
- Everything — themes, colors, layouts — implemented natively in
  Kotlin/XML, no WebView, no wrapped web app

## Building

This project has no committed Gradle wrapper jar (kept out on purpose — see
below). The included GitHub Actions workflow (`.github/workflows/build.yml`)
builds it for you automatically:

1. Push this project to a GitHub repo.
2. GitHub Actions will assemble both a debug and an unsigned release APK on
   every push to `main`.
3. Download the APK from the **Actions → workflow run → Artifacts** section.

If you want to build locally with Android Studio, just open the project —
Android Studio will generate its own wrapper automatically. There's nothing
Termux-specific required to open/build in Studio; the CI path is there for
building straight from Termux without a local JDK/Android SDK.

## Project structure

```
app/src/main/java/com/lukasosstudios/localnotes/
├── LocalNotesApp.kt          Application entry point (applies cached theme)
├── model/                    Note, NoteColor, filters, sort/theme enums
├── data/                     NoteRepository, SettingsRepository (file I/O)
├── util/                     Date formatting, theme application
└── ui/
    ├── notes/                MainActivity + NotesAdapter (the notes list)
    ├── editor/                NoteEditorActivity
    ├── calculator/            CalculatorActivity (expression evaluator)
    └── settings/              SettingsActivity
```

## Icon

The launcher icon is generated from the supplied artwork: a legacy square
icon for pre-Android-8 launchers, and a proper split adaptive icon
(navy background layer + notebook foreground layer, scaled to fit Android's
safe zone) for modern launchers, so it won't get clipped oddly by circular,
squircle, or square icon masks.
