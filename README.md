# Lyd Slice Mobile

Native Android 3D-print preparation app for importing STL models, arranging a build plate, configuring print settings, slicing, and sending G-code to OctoPrint.

## Current status

The app now integrates an ARM64 OrcaSlicer-derived native engine for local STL slicing, with an interactive 3D model view and G-code toolpath preview. This is an early integration: multiple STL files can be arranged and composed into one plate job, while interactive manual positioning and Robo 3D R1 Plus machine limits/start-end G-code still require further verification.

## Target hardware

- Android 16 (API 36)
- Samsung Galaxy Tab S10 Lite
- Robo 3D R1 Plus
- OctoPrint

## Project structure

- `app/` — Android application and Compose UI
- `app/src/main/java/com/lydiaperspective/lydslice/data/` — printer, model, and print-setting data types
- `app/src/main/java/com/lydiaperspective/lydslice/ui/` — application screens and theme
- `docs/ROADMAP.md` — implementation plan and technical decisions

## Open in Android Studio

1. Clone this repository.
2. Open the repository root in Android Studio (a recent stable version with Android 16 SDK support is recommended).
3. Install Android SDK Platform 36 and the Android SDK Build-Tools requested by Gradle.
4. Sync the project and run the `app` configuration on an Android 16 device or emulator.

The Gradle wrapper JAR is not included in this initial scaffold. If Gradle is installed locally, run `gradle wrapper` from the project root to generate wrapper files, then commit them.

## Print profiles and tuning

The settings screen includes built-in Robo R1 Plus starting profiles for PLA balanced/detail/draft and ABS low-cooling use, plus editable custom profiles saved on the device. A quality-versus-speed control adjusts layer height and print speed together, and printer/material-aware guidance warns about settings outside documented starting ranges. Presets are advisory starting points, not validated G-code recipes.

See [docs/PRINTER-PROFILE.md](docs/PRINTER-PROFILE.md) for the research notes, profile values, caveats and source links.

## Printer profiles

The Printer tab lets you select a built-in machine preset or create and save custom printer profiles. The initial presets include the Robo 3D R1 Plus and a generic FFF printer. Editing a built-in preset automatically creates a custom copy; it does not overwrite the original. Printer configuration includes build volume and plate geometry, heated-bed/chamber options, extruders and nozzle sizes, filament diameter, temperature ceilings, G-code flavor, printhead clearances, gantry height, and start/end G-code.

See [docs/PRINTER-SETTINGS.md](docs/PRINTER-SETTINGS.md) for the Cura-inspired settings list, preset caveats, and current implementation boundaries.

## Slicing engine and preview

- The app uses the pinned OrcaSlicer Mobile `libslic3r` ARM64 native engine through its JNI ABI.
- Android builds fetch a checksum-verified engine archive from the Feresa Slicer project during `preBuild`; the native binaries are not committed to this repository.
- The Workspace includes an interactive 3D STL preview and switches to a layer/toolpath preview after slicing.
- Multiple STL files are parsed and composed into a single binary STL plate before native slicing. Binary and ASCII STL input are supported.
- Auto-arrange uses a deterministic shelf-packing layout with 3 mm spacing, checks the configured rectangular build volume, and updates the plate preview.
- The plate composer supports stored XY centre positions, Z rotation and Z offset; interactive touch controls for those transforms are still pending.
- The composer currently limits a plate to 100 models and 2 million triangles total. Circular/custom build plates are not supported by auto-arrange.
- The native engine currently targets `arm64-v8a` and Android API 28+.
- G-code output is local and can be exported through Android's Save File picker.
- Slicing performs preflight checks for supported rectangular beds, positive machine dimensions, nozzle/filament values, temperature ceilings, layer height, and speed/infill ranges.
- The model is centered on the configured plate, placed on Z=0, and rejected if its bounds exceed the configured build volume.
- Generated output is checked for actual G-code movement commands before the app marks slicing as successful.
- The adapter emits warnings when printer start/end G-code is missing and while the R1 Plus profile remains unverified.

The OrcaSlicer-derived integration is distributed under AGPL-3.0. See `LICENSE`, `licenses/feresa/NOTICE.md`, and `licenses/feresa/THIRD_PARTY_NOTICES.md`. The app must preserve the corresponding source and license obligations when redistributed.

Generated G-code must be inspected in the preview and checked against the physical R1 Plus firmware, dimensions, temperatures, motion limits, and start/end G-code before printing.

## Safety

Never send generated G-code to a printer until it has been checked for the correct machine dimensions, temperatures, motion limits, start/end G-code, and firmware compatibility.
