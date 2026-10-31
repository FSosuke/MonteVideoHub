# MonteVideoHub

Werbefreie Musik-App für Android (optimiert für das **Sony Xperia 5 V**), die
**YouTube / YouTube Music und niconico gleichzeitig durchsucht** – im Design von
**Montedio Yamagata** (山形一心 · 共に、頂へ).

## Funktionen

- 🔎 **Kombinierte Suche** – YouTube und niconico parallel, Ergebnisse gemischt
  (Filter: Alle / YouTube / niconico, Schalter „Nur Musik“)
- 🎵 **Nur Audio** – keine Werbung, weniger Datenverbrauch, Videospuren werden gar nicht geladen
- 📱 **Hintergrundwiedergabe** mit Benachrichtigung, Sperrbildschirm- & Kopfhörersteuerung (Media3)
- ⏭️ **SponsorBlock** (wie bei ReVanced) – überspringt bei YouTube automatisch
  Nicht-Musik-Teile in Musikvideos, Sponsoren und Eigenwerbung
- 📜 **Warteschlange** – „Jetzt spielen“, „Als Nächstes“, „Zur Warteschlange“, Zufall, Wiederholen
- 💙💛 **Montedio-Design** – Nachtblau, Vereinsblau, Gelb; Stadion-/Bergbild als Hintergrund

## Technik

| Bereich | Bibliothek |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Player | AndroidX Media3 (ExoPlayer, HLS, MediaSessionService) |
| YouTube | [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) |
| niconico Suche | offizielle Snapshot-Such-API v2 |
| niconico Wiedergabe | Watch-API + „domand“-HLS (verschlüsselt, Cookie `domand_bid`) |
| Netzwerk / Bilder | OkHttp, Coil |

```
app/src/main/java/com/fsosuke/montevideohub/
├── data/        Http, YouTubeSource, NicoSource, SearchRepository, SponsorBlock
├── playback/    PlaybackService, PlayerConnection, SponsorSkipper, MediaItems
└── ui/          Compose-Screens (Suche, Mini-Player, Player) + Montedio-Theme
```

## APK bekommen

Jeder Push baut über **GitHub Actions** automatisch eine Debug-APK:
*Actions → „Build APK“ → letzter Lauf → Artefakt `MonteVideoHub-debug`* herunterladen,
entpacken und auf dem Xperia installieren („Unbekannte Apps installieren“ erlauben).

Lokal: Android Studio (JDK 17) öffnen oder `./gradlew assembleDebug`.

## Hinweise

- YouTube ändert regelmäßig seine Schnittstellen. Wenn die Wiedergabe bricht, zuerst die
  Version von `NewPipeExtractor` in `app/build.gradle.kts` aktualisieren.
- Manche niconico-Videos sind nur mit Login/Premium abspielbar.
- Privates Fan-Projekt – kein offizielles Produkt von Montedio Yamagata. Das Hintergrundbild
  und Vereinslogo gehören ihren Rechteinhabern; die App nicht im Play Store veröffentlichen.
