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
- 220 × 220 × 250 mm build volume (provisional; verify the actual usable travel)
- Rectangular build plate
- Heated bed
- Single extruder
- 0.4 mm nozzle
- 1.75 mm filament
- Marlin G-code flavor (confirm against the firmware installed on the actual printer)
- 290°C hotend and 100°C bed limits are configuration ceilings, not recommended print temperatures

The build volume and firmware configuration must be verified on the specific printer before production G-code is generated. R1 Plus machines may have been modified.

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

When the selected printer has no heated bed, the print-settings screen hides the bed-temperature control. Printer temperature ceilings also bound the corresponding print-setting controls.

## Cura reference

The configuration categories are based on Cura's documented custom-printer setup, which describes build dimensions, build-plate shape and heating, printhead clearances, extruder count, and machine start/end G-code. Cura separates machine-level settings from extruder-specific settings and allows printer definitions to be layered with user customizations.

- [UltiMaker Support: Add a custom printer to Cura](https://support.ultimaker.com/s/article/1667411002602)
- [Cura Wiki: Definition files explained](https://github.com/Ultimaker/Cura/wiki/Definition-Files-Explained)
- [Cura Wiki: Container stacks](https://github.com/Ultimaker/Cura/wiki/Container-Stacks)
- [Cura Wiki: Start and end G-code](https://github.com/Ultimaker/Cura/wiki/Start-End-G%E2%80%90Code)

## Current implementation boundary

These settings are stored and displayed by the app, and some of them constrain the print-settings UI. The STL slicing engine does not yet consume this machine profile. In particular, build plate geometry, head clearances, extruder count, G-code flavor, start/end scripts, and firmware limits must be connected to and validated by the slicing engine before the app can generate safe, machine-specific G-code.

Do not send generated G-code to a printer until the profile has been verified against the actual printer and firmware.
