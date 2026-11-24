# Printer setup and machine profiles

The Printer tab uses a machine-profile model inspired by UltiMaker Cura's machine and extruder configuration.

## Profile behavior

- Built-in profiles are immutable templates.
- Selecting a built-in profile fills the active machine configuration.
- Editing any value on a built-in profile creates a separate custom copy. The built-in remains unchanged.
- Custom profiles are saved locally on the Android device and the selected profile is restored on the next launch.
- Users can create, rename, edit, select and delete custom printer profiles.

## Included presets

### Robo 3D R1 Plus

The initial R1 Plus preset is a starting point:
- Firmware axis travel of X 240 × Y 254 × Z 240 mm; this is not confirmation of the protected bed's usable printable footprint
- Rectangular build plate
- Heated bed
- Single extruder
- 0.4 mm nozzle
- 1.75 mm filament
- Marlin G-code flavor (confirm against the firmware installed on the actual printer)
- 290°C hotend and 110°C bed limits are firmware configuration ceilings, not recommended print temperatures

The firmware axis limits come from the installed R1+ firmware configuration. Measure the protected bed's actual usable surface and verify unobstructed travel on the specific printer before printing near the edges. R1 Plus machines may have been modified.

### Generic FFF Printer

A generic starting template is provided with placeholder 200 × 200 × 200 mm dimensions, one extruder, a 0.4 mm nozzle, 1.75 mm filament, and no heated bed. Replace every placeholder with the actual machine specifications.

## Editable machine settings

The editor currently exposes:

- Printer profile name
- Build volume X width, Y depth, and Z height
- Build plate shape: rectangular, circular, or custom
- Origin at center (0,0)
- Heated bed and heated build volume/chamber
- Number of extruders
- Supported nozzle sizes and currently installed nozzle
- Filament diameter
- Maximum nozzle and bed temperatures
- G-code flavor
- Printhead clearance toward X/Y minimum and maximum
- Gantry height
- Machine start and end G-code
- Optional post-levelling nozzle purge / wipe G-code (user-supplied; no wipe coordinates are assumed)

When the selected printer has no heated bed, the print-settings screen hides the bed-temperature control. Printer temperature ceilings also bound the corresponding print-setting controls. The optional nozzle-preparation script is appended after the machine start script, so on the R1 Plus it runs after G28/G29 and the configured extrusion reset.

## Bed adhesion and first-layer controls

The print-settings screen exposes OrcaSlicer-backed adhesion choices: none, skirt, brim, and raft. Brim width, raft layer count, and skirt loop count are configured independently. First-layer tuning includes outer-wall speed, infill speed, flow ratio, and the number of initial layers for which the part-cooling fan is kept off; the normal fan setting applies after those layers. The existing first-layer speed control remains available.

For the R1 Plus, the built-in start script retains G28 followed by G29. This performs firmware homing and automatic bed levelling before the optional nozzle-preparation script and print. G29 is not a substitute for a clean bed, correct probe offset, or a clean nozzle. The app does not assume a physical nozzle brush or a safe purge-line location. Only configure purge/wipe moves after verifying the actual coordinates and clearance on the individual printer. Inspect the exported G-code before printing, especially after changing start scripts.

First-layer flow is a fine-tuning control, not a fix for incorrect Z-probe offset, dirty build surface, or damaged/uneven bed covering. Start near 100% and adjust in small steps based on a test print. Follow the filament manufacturer's temperature guidance; a brim or raft cannot compensate for poor surface preparation.

## Cura reference

The configuration categories are based on Cura's documented custom-printer setup, which describes build dimensions, build-plate shape and heating, printhead clearances, extruder count, and machine start/end G-code. Cura separates machine-level settings from extruder-specific settings and allows printer definitions to be layered with user customizations.

- [UltiMaker Support: Add a custom printer to Cura](https://support.ultimaker.com/s/article/1667411002602)
- [Cura Wiki: Definition files explained](https://github.com/Ultimaker/Cura/wiki/Definition-Files-Explained)
- [Cura Wiki: Container stacks](https://github.com/Ultimaker/Cura/wiki/Container-Stacks)
- [Cura Wiki: Start and end G-code](https://github.com/Ultimaker/Cura/wiki/Start-End-G%E2%80%90Code)

## Current implementation boundary

The OrcaSlicer adapter consumes the configured rectangular build area, origin mode, build height, nozzle and filament diameters, temperature ceilings, G-code flavor, start/end scripts, and R1+ Z-probe offset. Head clearances and multi-extruder machine behavior are currently profile metadata and are not fully implemented in slicing. Circular/custom build plates are rejected. Firmware axis limits do not establish the protected printable footprint.

Do not send generated G-code to a printer until the profile has been verified against the actual printer and firmware.
