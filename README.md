# ROM Explorer

A personal, local-only Android app for browsing your own ROM library the way
retroexplore.com displays its catalog (System / Genre / "Popular only"
filters, box art grid), with RetroAchievements progress tracking and
one-tap launch into whatever emulators you already have installed
(same handoff model as iiSU / ES-DE / Daijishou).

Nothing here downloads or distributes ROMs. It only indexes files you already
have and points at metadata you already have accounts for.

## What it does

- **Library scan**: pick a folder via the system file picker (Storage Access
  Framework, so it works fine under Android's scoped storage). Recursively
  finds ROMs by extension, guesses the system (NES/SNES/GBA/PS1/PS2/N64/...),
  and cleans up filenames into display titles.
- **RetroExplore-style browsing**: filter by System, Genre, "Popular only",
  free-text search, and sort by title / popularity / system / achievement
  count - same filter set as the site's own browse page.
- **RetroExplore metadata**: on demand (tap the refresh icon), looks up each
  game's genre, box art, and description from retroexplore.com. This is a
  best-effort HTML scrape (the site has no public API), cached locally in
  Room so each game is only fetched once. If RetroExplore changes their page
  markup, update the selectors in `RetroExploreClient.kt`.
- **RetroAchievements**: enter your own username + Web API key (Settings) -
  the same official API RetroArch/iiSU use. The app MD5-hashes each ROM and
  looks up achievement counts / your unlock progress.
- **Emulator launch (iiSU-style handoff)**: "Play" hands the ROM's content
  Uri to the first installed emulator that supports that system (RetroArch,
  PPSSPP, Dolphin, DuckStation, Citra, etc.). No emulation code is bundled.

## Known limitations (be aware before you rely on this)

- **RA hashing**: RetroAchievements' canonical hash rules differ per console
  (some strip headers, disc-based systems hash specific tracks, etc.). This
  build implements the simple case - MD5 of the raw file, with iNES header
  stripping for NES - which is correct for many cartridge systems but **will
  not match** for PS1/PS2/Saturn/Dreamcast/PSP and similar disc-based systems.
  Large disc images (>64MB) are skipped for hashing entirely rather than
  loading them fully into memory. Extend `RaHasher` in
  `RetroAchievementsClient.kt` if you want to add a console's real algorithm
  (RA's hashing spec is documented at https://docs.retroachievements.org).
- **RetroExplore lookups** are a plain HTML scrape of search results, not an
  official API - matching is "first search result for the cleaned filename,"
  which won't always be right for obscure titles, romhacks, or ambiguous
  names. You can always tap into a game and it'll just show what it found.
- **Emulator list** in `EmulatorLauncher.kt` covers the common ones; add more
  by package name as needed.

## Building it

You need Android Studio (free) or the workflow below - I can't compile an
APK from here since doing so needs the Android SDK / Google's Maven repo,
which isn't reachable from this sandboxed environment.

### Option A - Android Studio (fastest, gives you a signed/installable APK)

1. Install Android Studio: https://developer.android.com/studio
2. File -> Open -> select this `RomExplorer/` folder.
3. Let Gradle sync (Studio will offer to generate the wrapper jar
   automatically the first time - accept it).
4. Plug in your phone (USB debugging on) or start an emulator, hit Run.
   Or Build -> Build Bundle(s)/APK(s) -> Build APK(s) to just get the file.

### Option B - GitHub Actions (no Android Studio needed)

1. Push this folder to a new GitHub repo (can be private).
2. The included `.github/workflows/build-apk.yml` runs automatically on
   push and builds a debug APK on GitHub's servers.
3. Open the workflow run under the Actions tab -> download the
   `rom-explorer-debug-apk` artifact -> transfer to your phone -> install
   (enable "install unknown apps" for whatever app you use to open it).

Either way this produces a **debug-signed APK**, fine for installing on your
own device. If you want a proper release build, generate a signing key in
Android Studio (Build -> Generate Signed Bundle/APK) - a debug key can't be
used for anything you'd distribute, but that's not a concern here since it's
just for you.

## Project layout

```
app/src/main/java/com/romexplorer/app/
  data/       Room entities/DAO, DataStore settings, LibraryRepository
  scan/       SAF folder walker + extension->system detection
  network/    RetroExplore scraper client, RetroAchievements API client + hasher
  emulator/   Intent-based launch into installed emulators
  ui/         Compose screens (library grid, game detail, settings)
```

## Permissions

- Storage access is scoped to the single folder you pick via SAF - no
  broad storage permission needed.
- `INTERNET` is used only for the optional RetroExplore/RetroAchievements
  lookups you trigger yourself.
- `QUERY_ALL_PACKAGES` is used to check which emulators are installed so
  Play can pick one; you can tighten this to just the `<queries>` block
  already declared in the manifest if you'd rather drop that permission
  (remove the `uses-permission` line, the `<queries>` list alone is enough
  on modern Android).
