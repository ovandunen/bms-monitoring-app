# EcoCar GUI (Compose Multiplatform)

Eigenständiges Gradle-Projekt im Unterordner `eco-car-gui/` (nicht per `:eco-car-gui` aus dem BMS-Android-Root eingebunden). **Kotlin Multiplatform** mit gemeinsamer UI in `:composeApp` und Android-Start über `:androidApp`. Desktop bleibt über `Main.kt` + `compose.desktop` nutzbar.

4-Zonen-Layout gemäß Wireframe **GUI_EcoCar_01v02** (TopBar, linke Sidebar, Hauptinhalt, BottomBar): Farben Near-Black / Dark-Green / Golden-Yellow, sechs Nav-Tiles, kollabierbare Leisten, Telemetrie in der BottomBar, Low-Battery-Dialog.

## Module layout

| Modul | Inhalt |
|--------|--------|
| `:composeApp` | KMP-Library: `commonMain` (u. a. `EcoCarApp`), `androidMain` (Karte via **MapLibre Compose** `maplibre-compose-android`), `desktopMain` (`Main.kt`) |
| `:androidApp` | Android Application: `AndroidManifest.xml`, `MainActivity`, ProGuard |

| Bereich | Composables / Dateien |
|--------|----------------|
| App shell | `composeApp/.../ui/EcoCarApp.kt`, `ui/AppScaffold.kt` |
| Theming | `theme/EcoCarColors.kt`, `ui/theme/EcoCarTheme.kt` |
| Navigation | `nav/MainDestination.kt` |
| Karte (Android) | `map/EcoMapContent.android.kt` (`MaplibreMap` + MapTiler **streets-v2-dark**) |
| Einstieg Android | `androidApp/.../MainActivity.kt` |
| Einstieg Desktop | `composeApp/.../desktopMain/.../Main.kt` |

## MapTiler API-Key (nicht ins Repo committen)

1. In `local.properties` (wird von Git ignoriert) eine Zeile ergänzen:

   `maptiler.key=IHR_MAPTILER_API_KEY`

2. Vorlage: `local.properties.example`

Der Key wird beim Build in `BuildConfig.MAPTILER_KEY` der Library `:composeApp` geschrieben und nur in der Style-URL verwendet.

## Build & Run

- **Kotlin** 2.1.x, **Java-Ziel** 17 (Gradle **JVM Toolchain 17**; Auto-Download über Foojay, falls lokal kein JDK 17 installiert ist).
- **Android:** `compileSdk` / `targetSdk` **36**, `minSdk` **23**.
- Auf dem Build-Rechner läuft Gradle **9.1** (Kompatibilität mit **JDK 25** als Gradle-JVM); die App selbst zielt weiter auf **17**.

```bash
cd eco-car-gui
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:run
```

## Android Studio: Head-Unit 1280×720, Landscape

**Kein Modul in der Run-Konfiguration?** Android Studio muss den **Gradle-Root `eco-car-gui/`** geöffnet haben (nicht nur den übergeordneten `bms-monitoring-app/`-Ordner — dort gibt es nur `:app`). Danach **Gradle neu laden** (Elefant-Symbol → *Reload All Gradle Projects*). In der Run-Konfiguration Modul z. B. **`androidApp`** wählen (Variante **main**).

1. **Device Manager** → **Create device** → **New hardware profile**: Resolution **1280 × 720**, optional RAM nach Bedarf.
2. System image passend zu **API 36** wählen (oder niedrigeres Image, solange `minSdk` erfüllt ist).
3. AVD bearbeiten: **Startup orientation** und/oder **Advanced** so setzen, dass die Oberfläche **landscape** startet (entspricht der `screenOrientation` in `AndroidManifest`).
4. Run-Konfiguration **„EcoCar Android (landscape)”** (`.idea/runConfigurations/EcoCar_Android_HeadUnit_1280x720.xml`) nutzt das Modul `androidApp`; Zielgerät im Dialog den angelegten **1280×720**-AVD wählen.

## Kurzbefehl

- **F9**: Low-Battery-Dialog öffnen  
- Zusätzlich: Tab **Settings** → Button „Low-Battery-Dialog testen“

## BMS-Haupt-Android-App

Das produktive BMS-Android-Projekt liegt im **Repository-Root** (`./gradlew :app:…`). EcoCar-GUI in diesem Unterordner ist **eigenständig** (`./gradlew :androidApp:…`).
