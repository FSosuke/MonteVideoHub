# MonteVideoHub

Werbefreie Musik-App für Android (optimiert für das **Sony Xperia 5 V**), die
**YouTube / YouTube Music und niconico gleichzeitig durchsucht** – im Design von
**Montedio Yamagata** (山形一心 · 共に、頂へ).

## Funktionen

- 🏠 **Startseite** wie bei YouTube/niconico – YouTube-Trends, niconico-Rankings (Musik & Gesamt),
  „Zuletzt gespielt“, Filter-Chips, zum Aktualisieren nach unten ziehen
- ⬇️ **Downloads** als Audio (.m4a) oder Video (.mp4) in einen **selbst gewählten Ordner**
  (fragt auf Wunsch jedes Mal), offline abspielbar, Fortschritt in der Benachrichtigung
- ⚙️ **Einstellungen** – Konten, Videoqualität, SponsorBlock, Download-Ordner, Region, Updates
- 👤 **niconico-Login** (offizielle Login-Seite in der App) für Login-Videos & Premium-Qualität

- 🔎 **Kombinierte Suche** – YouTube und niconico parallel, Ergebnisse gemischt
  (Filter: Alle / YouTube / niconico, Schalter „Nur Musik“)
- 🎬 **Video oder nur Audio** – umschaltbar im Player (Kamera/Kopfhörer-Symbol). Video bis 1080p,
  Vollbild im Querformat mit Zoom auf das 21:9-Display; im Audio-Modus wird kein Video geladen
- 🚫 **Keine Werbung** – Streams werden direkt abgespielt
- 📱 **Hintergrundwiedergabe** mit Benachrichtigung, Sperrbildschirm- & Kopfhörersteuerung (Media3)
- ⏭️ **SponsorBlock** (wie bei ReVanced) – überspringt bei YouTube automatisch
  Nicht-Musik-Teile in Musikvideos, Sponsoren und Eigenwerbung
- 📜 **Warteschlange** – „Jetzt spielen“, „Als Nächstes“, „Zur Warteschlange“, Zufall, Wiederholen
- 💙💛 **Montedio-Design** – Nachtblau, Vereinsblau, Gelb; Berg-/Stadtbild als Hintergrund;
  Schrift **Noto Sans JP** wie auf montedioyamagata.jp
- 🔄 **In-App-Updates** wie bei ReVanced Manager / NewPipe – Update-Symbol oben rechts

## Technik

| Bereich | Bibliothek |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Player | AndroidX Media3 (ExoPlayer, HLS, MediaSessionService) |
| YouTube | [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) |
| niconico Suche | nvapi `v2/search/video` (wie die Suchseite) |
| niconico Wiedergabe | Gast-Watch-API v3 + „domand“-HLS (verschlüsselt, Cookie `domand_bid`) |
| Netzwerk / Bilder | OkHttp, Coil |

```
app/src/main/java/com/fsosuke/montevideohub/
├── data/        Http, YouTubeSource, NicoSource, SearchRepository, SponsorBlock
├── playback/    PlaybackService, PlayerConnection, SponsorSkipper, MediaItems
└── ui/          Compose-Screens (Suche, Mini-Player, Player) + Montedio-Theme
```

## APK & Updates

Jeder Push baut über **GitHub Actions** eine signierte APK und veröffentlicht sie als
**GitHub-Release** (`b<Build-Nummer>`). Die App prüft beim Start die neueste Release und bietet
das Update direkt an (Download + Installation über den Android-Paketinstaller).

Erstinstallation: APK unter **Releases** herunterladen und installieren
(„Unbekannte Apps installieren“ erlauben). Danach kommen Updates in der App.

Die CI führt außerdem einen **Live-Check** aus (`LiveSourcesTest`): echte Suche + Stream-Abruf bei
YouTube und niconico. Ergebnis im Schritt „Live-Check“ des Actions-Laufs.

Lokal: Android Studio (JDK 17) öffnen oder `./gradlew assembleDebug`.

### Signatur (einmalig einrichten)
Damit Updates über die installierte Version passen, braucht jede APK denselben Schlüssel.
Er liegt **nicht** im Repo, sondern in zwei Actions-Secrets
(*Settings → Secrets and variables → Actions → New repository secret*):

| Secret | Inhalt |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Keystore (PKCS12, Alias `montevideohub`) als Base64 |
| `SIGNING_PASSWORD` | Passwort für Keystore und Schlüssel |

Ohne Secrets baut die CI weiter, signiert aber mit einem Wegwerf-Schlüssel und erstellt keine Release.

## Hinweise

- YouTube ändert regelmäßig seine Schnittstellen. Wenn die Wiedergabe bricht, zuerst die
  Version von `NewPipeExtractor` in `app/build.gradle.kts` aktualisieren.
- Manche niconico-Videos sind nur mit Login/Premium abspielbar.
- Schrift: Noto Sans JP, SIL Open Font License (`licenses/`). Die Überschriften der Vereinsseite
  nutzen „Avenir“ – kommerziell lizenziert, daher nicht enthalten.
- Privates Fan-Projekt – kein offizielles Produkt von Montedio Yamagata. Das Hintergrundbild
  und Vereinslogo gehören ihren Rechteinhabern; die App nicht im Play Store veröffentlichen.
