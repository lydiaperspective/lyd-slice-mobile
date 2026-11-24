# Robo 3D R1 Plus printer profile

## Hardware facts and confidence

The R1+ is a single-extruder printer with a heated bed. Robo's support guidance gives PLA temperatures of 190–210°C at the nozzle and 50–70°C at the bed, and ABS temperatures of 230–250°C at the nozzle and 80–100°C at the bed. Published R1+ hardware specifications describe a 0.4 mm nozzle, 1.75 mm filament and a hot-end ceiling of 290°C; the 290°C value is a hardware limit, not a recommended print temperature.

Marlin's published R1+ machine entry identifies a Cartesian machine with one extruder, a RAMPS 1.4 EFB board and a nominal build volume of 240 × 254 × 240 mm. The app now uses those dimensions rather than the earlier provisional 220 × 220 × 250 mm values. This is a firmware reference, not proof that every used machine has the same unobstructed travel: clips, bed changes, endstop positions, replacement firmware and other modifications can reduce or alter usable space.

Robo's MatterControl support guidance explicitly says R1 and R1+ use firmware bed leveling and instructs users to place G29 after the other start-G-code lines. The preset therefore homes with G28 and runs G29. Robo's Z-offset article says the R1+ offset is adjusted through its controls/macros and distinguishes it from the older R1; the app deliberately does not inject an M565 offset.

## Built-in starting profiles

The built-in R1+ machine profile now starts with G21, G90, M83, temperature set/wait commands using OrcaSlicer's initial-layer placeholders, G28, G29, and G92 E0. Its end script turns off the hot end, heated bed and part fan, then disables the motors. The slicer is explicitly configured for relative E distances to match M83.

These scripts are a conservative baseline based on Robo's documented firmware-level leveling workflow and OrcaSlicer's Marlin/relative-extrusion conventions. The exact firmware revision and any owner modifications on an individual printer are not known; inspect the exported G-code and verify the printer's firmware behavior before printing.

These are cautious starting presets for experimentation, not official factory profiles and not guaranteed recipes. The nozzle and bed temperature bands come from Robo's R1+ temperature guidance. Speeds and quality combinations are conservative app defaults informed by the manual's 0.2 mm default and the documented trade-off between layer height, print time and detail.

| Profile | Layer height | Print speed | Nozzle | Bed | Fan | Intended use |
|---|---:|---:|---:|---:|---:|---|
| PLA · Balanced | 0.20 mm | 40 mm/s | 205°C | 60°C | 100% | General-purpose starting point |
| PLA · Fine detail | 0.12 mm | 30 mm/s | 200°C | 60°C | 100% | Slower, finer layers |
| PLA · Faster draft | 0.27 mm | 50 mm/s | 205°C | 55°C | 100% | Less detail, shorter print |
| ABS · Ventilated / low cooling | 0.20 mm | 35 mm/s | 240°C | 95°C | 0% | ABS where drafts and temperature are controlled |

Robo's temperature guidance:
- PLA: 190–210°C nozzle and 50–70°C bed.
- ABS: 230–250°C nozzle and 80–100°C bed.
- The advertised 290°C hot-end maximum is a hardware capability, not a recommended everyday setpoint.

Robo's troubleshooting guidance recommends lowering the fan to around 30% if parts curl, using no part cooling for ABS, and slowing/thickening the first layer when bed adhesion is poor. These are troubleshooting suggestions, not universal requirements.

## Owner-reported experience and caveats

A long-term R1+ owner-maintainer's project notes that their machine had been modified over several years. They report that dual part-cooling fans improved PLA appearance and that careful X-axis gantry balance/mesh leveling had a noticeable effect on first-layer quality. The same owner reports personally preferring MatterSlicer results to Cura on their setup. This is useful context, but it is one modified machine and does not establish a universal speed/temperature recipe.

Robo's own support material also contains differing Z-offset guidance across pages: one operation guide describes a typical R1+ offset around 0.9 mm, while a separate Z-endstop support article says to leave the R1 Plus at 0 mm in a particular Cura/start-G-code workflow. Because the instructions are context-dependent and potentially firmware/slicer-specific, the app must not hard-code a Z-offset or start G-code. The actual printer's firmware, slicer profile and first-layer calibration need to be checked.

## In-app warning rules

The app warns when PLA or ABS temperatures fall outside Robo's published material guidance, ABS part cooling is high, PLA cooling is unusually low, layer height exceeds 0.30 mm, print speed exceeds the app's conservative 60 mm/s starting range, initial-layer speed is high, or the quality/speed control is pushed strongly toward speed.

These are advisory warnings. Filament manufacturers' instructions and the condition/calibration of the actual printer take precedence.

## Before enabling production G-code

Confirm from the specific printer/firmware configuration:

- Actual usable X/Y/Z travel and origin convention (the app's 240 × 254 × 240 mm values come from Marlin's R1+ reference configuration)
- Nozzle diameter and filament diameter
- Heated-bed availability and temperature limits
- Hotend temperature limits
- Maximum feed rates, acceleration and jerk settings
- Firmware flavor and supported G-code commands
- Start G-code, end G-code, homing and bed-leveling behavior (the preset uses G28 followed by G29, as documented for R1+)
- Whether the printer has been modified (hot end, fans, extruder, bed, firmware)

Do not assume all R1 Plus machines have identical firmware or modifications. Keep machine limits and start/end G-code editable per printer profile.

## Sources

- Marlin Firmware, [Robo3D R1+ machine reference](https://marlinfw.org/docs/appendix/printers/robo3d.html)
- MatterHackers, [Using firmware leveling with Robo R1/R1+](https://help.matterhackers.com/article/62-i-updated-mattercontrol-and-now-i-cant-see-software-print-leveling-in-the-options-menu)
- Robo Help Center, [Adjusting temperatures](https://help.robo3d.com/hc/en-us/articles/115001254531-Adjusting-temperatures)
- Robo Help Center, [Z-axis endstop and R1+ Z-offset guidance](https://help.robo3d.com/hc/en-us/articles/115000724052-Z-Axis-endstop-fix)
- Robo Help Center, [Troubleshooting](https://support-site.readthedocs.io/en/master/R1Plus/troubleshooting/)
- Robo Help Center, [Speed settings](https://help.robo3d.com/hc/en-us/articles/115000716631-Speed)
- ROBO 3D, [FAQ](https://robo3d.com/pages/learn-faq)
- ROBO 3D R1 Plus manual, [manual copy](https://manuals.plus/m/87abab7905bbe0be626af8ae95557d75cd053bb90dffe219323b296514acca72_optim.pdf)
- B&H, [R1 Plus product specifications](https://www.bhphotovideo.com/c/product/1195134-REG/robo_3d_a1_0002_000_r1_plus_3d_printer.html/)
