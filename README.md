# Local Notes

A native Android notes app (Kotlin) with customizable note colors, a built-in
calculator, and plain-text storage you can actually see and back up.

No account, no cloud, no database file you can't read. Every note is a
plain-text file.

## Where your data lives

- **Notes:** `Android/media/com.lukasosstudios.localnotes/notes/*.txt` — one
  file per note. Each file has a tiny plain-text header (color,
  pinned/archived/deleted flags, timestamps) followed by the title and body,
  so you can open any note in a plain text editor and understand it
  immediately.
- **Settings:** `Android/media/com.lukasosstudios.localnotes/settings.properties`
  — theme and sort preference, mirrored here every time you change them in
  the app.

The app requests **All files access** (`MANAGE_EXTERNAL_STORAGE`) on first
launch so it can read and write that shared `Android/media` folder rather
than being locked into its own private sandbox.

## Features

- Pin, archive, and soft-delete (trash) notes, with a dedicated trash view
  and "empty trash" action
- Long-press any note to enter multi-select mode for bulk pin/archive/trash
  (and restore/delete-forever inside the Trash view)
- Markdown-lite: `**bold**`, `*italic*`, and `- [ ]` / `- [x]` checklists,
  rendered visually in both the editor and the notes list, tap a checkbox to
  toggle it -- the file on disk always stays plain, human-readable markdown
- Ten note colors (Butter, Sky, Sage, Rose, Iris, Peach, Mint, Slate, Sun,
  Coral) plus a genuine custom color picker (RGB sliders + hex input) for
  anything the presets don't cover
- One-tap backup: zips the whole notes + settings folder and hands it to the
  share sheet; import restores from a picked `.zip`
- Optional app lock using your device's own fingerprint/face/PIN/pattern
  (no separate PIN is stored by the app -- it defers entirely to Android's
  BiometricPrompt), toggle it from Settings
- Full light/dark/system theme support
- Sort by last edited, date created, or title
- Search across title and body
- A genuinely offline calculator with parentheses support
- Everything -- themes, colors, layouts -- implemented natively in
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
├── LocalNotesApp.kt          Application entry point (theme + lock arming)
├── model/                    Note, NoteColor, filters, sort/theme enums
├── data/                     NoteRepository, SettingsRepository, BackupManager
├── util/                     Date formatting, theme, markdown, app-lock helpers
└── ui/
    ├── notes/                MainActivity + NotesAdapter (list + multi-select)
    ├── editor/                NoteEditorActivity (markdown, colors)
    ├── calculator/            CalculatorActivity (expression evaluator)
    ├── settings/              SettingsActivity (appearance, backup, security)
    └── security/              LockActivity (biometric/PIN gate)
```

## Icon

The launcher icon is generated from the supplied artwork: a legacy square
icon for pre-Android-8 launchers, and a proper split adaptive icon
(navy background layer + notebook foreground layer, scaled to fit Android's
safe zone) for modern launchers, so it won't get clipped oddly by circular,
squircle, or square icon masks.
