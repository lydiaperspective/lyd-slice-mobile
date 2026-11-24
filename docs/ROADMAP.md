# Development roadmap

## Phase 0 — Android foundation
- [x] Native Kotlin / Jetpack Compose project
- [x] Tablet-friendly navigation shell
- [x] Multiple STL document selection
- [x] Initial print-setting state model and controls
- [x] Robo 3D R1 Plus profile data type
- [ ] Build and run verification in Android Studio
- [x] Add an engine-facing slicing progress and G-code export workflow (requires a real engine implementation)

## Phase 1 — Model workspace
- [x] Add interactive 3D STL model preview
- Parse binary and ASCII STL files and calculate mesh bounds
- Persist imported files in app-managed storage
- [x] Add an interactive 3D viewport with orbit, pan and zoom
- Add model selection and touch gizmos
- [x] Support per-model XYZ placement and three-axis rotation in the plate composition data path
- [x] Validate model placement against the configured build volume
- [x] Implement deterministic shelf-based auto-arrangement with spacing and fit checks
- [x] Add manual numeric move/rotate controls for XYZ and all three rotation axes
- [x] Add six-orientation flat-contact geometry heuristic and larger-first plate packing
- [ ] Add interactive touch gizmos and collision-aware placement editing
- [ ] Compare sliced candidate orientations using support volume, overhang risk, and actual time/material estimates

## Phase 2 — Printer and material profiles
- Confirm the exact R1 Plus build volume and firmware-specific limits
- Add nozzle, filament, temperatures, retraction, acceleration and start/end G-code settings
- [x] Add material presets and editable custom print profiles
- [x] Scope saved print profiles to individual printer profiles and support copying between printers
- Persist projects and settings

## Phase 3 — Slicing engine
- [x] Evaluate and select an Android-packaged OrcaSlicer-derived native engine
- [x] Define an engine adapter and typed slice request/result
- [x] Integrate the checksum-pinned ARM64 native engine and JNI ABI
- [x] Generate G-code from multiple STL files composed into one plate mesh and the current printer/print settings
- [x] Add an interactive G-code toolpath preview
- [x] Add the UI workflow for progress reporting, completion/failure feedback, engine warnings, time/material estimates, and G-code export
- [x] Add multi-model plate composition and automatic arrangement
- [ ] Add cancellation and slicer job interruption
- [ ] Verify the R1 Plus profile, start/end G-code and generated output against known-good test prints before printing

## Phase 4 — OctoPrint
- Configure server URL and API key
- Store credentials using Android Keystore-backed encrypted storage
- Test connection and retrieve printer/job status
- Upload G-code and optionally start a print only after explicit confirmation
- Display upload and print progress

## Design constraints
- Keep geometry, packing, slicing, and OctoPrint networking outside Compose UI code.
- Treat print-time optimization as an objective requiring a real slicer estimate; the current auto-orientation score is only a geometric heuristic and does not simulate supports or slice time.
- Never silently start a print. Upload and print-start should be separate user-confirmed actions.
