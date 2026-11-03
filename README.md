# Glint (working name)

Widgets + home-screen launcher for Android, with Focus mode. Free core, one Pro unlock,
nothing asked at install.

```
android/   Kotlin, Jetpack Compose, Glance. minSdk 31 (Android 12).
  app/         Settings: set as home, style, widget gallery (51), wallpapers (24),
               Dock settings, DND access
  core-theme/  Style system: Bento (default), Ink, Liquid; baked glass renderer
  core-data/   Weather by city, calendar, battery, steps sensor, now playing,
               next alarm, storage, moon phase
  focus/       Focus sessions, stats/streaks (unit-tested), DND control, end worker
  dock/        Dock mode: bedside clock (3 pages), tilt detector (unit-tested),
               screen saver, Quick Settings tile, alarms-only DND
  launcher/    Home screen (3 styles), app drawer + search, Focus home,
               setup sheet, blocked-app sheet (hold 3 s), session summary
  widgets/     One Glance engine, 19 widget types, 51 designs, setup screen,
               note/checklist editor
backend/   Go 1.24 API (catalogue, wallpapers, quotes, Play purchase verify).
           Lambda (provided.al2023, arm64) or docker compose + Postgres 16.
design/    HTML mockups of the UI and the three style directions.
.github/   CI: unit tests + debug APK artifact, Go vet/test/build.
```

## Run the app
Open `android/` in Android Studio and run `app`, or `./gradlew :app:assembleDebug`.
In the app tap **Set as home**. Long-press the home screen to come back to settings.

### Focus mode
- Start from the Focus tile, the Focus widget, or the dock.
- Choose intent, length (25/50/90/until I stop) and the apps that stay available.
- The launcher swaps to a calm timer screen; other apps are hidden.
- Opening a blocked app shows a sheet: hold 3 s to get a 5-minute pass.
- Notifications are silenced with Do Not Disturb (priority only) if access is granted.
- Limits: blocking applies to launches from Glint. Apps opened from notifications or
  recents are not blocked (that would need Accessibility, which we avoid on purpose).

### Widgets (51 designs, 19 types)
Clock (7), Analog (4), World clock (2), Date (4), Calendar (2), Weather (5), Battery (4),
Steps (3), Music (3), Note, Checklist, Quote (2), Countdown/progress (3), Quick apps (2),
Search, Photo (2), Focus (2), Next alarm, Moon phase, Storage.
- Add from the in-app gallery (exact design is pinned) or the launcher's widget list
  (setup screen opens, all optional).
- Clocks use TextClock/AnalogClock, so they tick without exact-alarm permission.
- Permissions are asked on the setup screen of the widget that needs one:
  Calendar, Physical activity (steps), Notification access (music). Nothing else.

### Wallpapers
24 originals (8 palettes × Mesh, Dunes, Orbit), drawn on the phone at the exact screen
size, so the APK stays small. Set home, lock or both. Uses SET_WALLPAPER (no prompt).

### Dock mode
Bedside clock like iPhone StandBy. Pages: Clock + month, Big clock, Agenda (alarm,
events, weather, music). Night mode turns it dim red; pixel shift every minute.
- Starts by itself when Glint home is open, the phone is charging, auto-rotate is on,
  and the phone is set on its side for ~2 s. Leaves when picked up or unplugged.
- On the lock screen: choose "Glint Dock" as the screen saver (Settings → Display →
  Screen saver, start while charging). Android only allows this route for showing
  over the lock screen without the user opening the app.
- Quick Settings tile "Dock" opens it by hand.
- While docked: Do Not Disturb alarms-only; the previous mode is restored after,
  unless the user changed it in the meantime.

### Weather
Uses Open-Meteo by city name (no location permission, no API key). Their free tier
is non-commercial: before a paid launch, buy their commercial plan or proxy through
the Go backend.

## Backend
```
cd backend
make dev     # Postgres + API on :8080
make test
make deploy  # sam build && sam deploy --guided
```
Play purchase verification is still a stub that rejects every purchase.
