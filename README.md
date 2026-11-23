# Lyd Slice Mobile

Native Android 3D-print preparation app for importing STL models, arranging a build plate, configuring print settings, slicing, and sending G-code to OctoPrint.

## Current status

This repository contains the initial Android application foundation. The current workspace is a UI and architecture scaffold; it does **not** yet generate production G-code or communicate with a printer.

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

## Slicing engine

A real slicer is a major next milestone. The app will use a dedicated engine adapter so the Android UI does not depend on a particular slicer implementation. G-code must be validated against the R1 Plus firmware and printer profile before being treated as print-ready.

## Safety

Never send generated G-code to a printer until it has been checked for the correct machine dimensions, temperatures, motion limits, start/end G-code, and firmware compatibility.
