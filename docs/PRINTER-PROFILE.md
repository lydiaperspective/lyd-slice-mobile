# Robo 3D R1 Plus printer profile

## Hardware facts and confidence

The owner-provided ctkjose/R1PLUSFIRMWARE repository is the primary reference for this printer's installed firmware. Its `FIRMWARE/config.h` sets X travel to 0–240 mm, Y travel to 0–254 mm and Z travel to 0–240 mm; it configures a 3 × 3 automatic bed-levelling grid with probe coordinates X 15–205 mm and Y 20–230 mm. The repository's `CFG_ROBOR1PLUS.md` sets `CFG_Z_PROBE_OFFSET_FROM_EXTRUDER` to 1 mm. Its `config_temp.h` sets the hot-end maximum to 290°C and bed maximum to 110°C, with one active hot-end thermistor and a heated-bed thermistor. The repository describes the board target as RAMPS 1.4/1.6 with an ATmega2560.

The firmware's X/Y travel limits are not proof of the usable printable surface. Its README mentions common 220 × 220 mm bed films/tapes, but does not define the physical protector's exact dimensions or its position relative to the home origin. The app's current X/Y plate fields are set to the firmware's 240 × 254 mm travel limits as a provisional envelope, not as a verified protected print surface. Measure the protector's printable footprint and set those fields to the safe usable area before slicing models near the edges. The reported ~1 mm protector thickness is not itself a reason to subtract 1 mm from Z height; the firmware's Z-probe offset is the relevant first-layer calibration setting.

This firmware is a Marlin-derived rewrite. Its README says its G-code behavior is closer to RepRapFirmware/LinuxCNC in some respects, and its Cura guidance uses `RepRap (Marlin/Sprinter)`. The app preset therefore selects OrcaSlicer's `RepRap` flavor rather than generic `Marlin`. The firmware source explicitly implements `G28`, `G29`, `M565`, `M82`, `M83`, `M104`, `M109`, `M140`, `M190`, `M106`, `M107` and `M84`.

## Built-in starting profiles

The built-in R1+ profile now uses OrcaSlicer's legacy Marlin flavor, relative extrusion, the firmware's 1 mm Z-probe offset (emitted as `M565 Z-1.0` before `G28`), then `G28` and `G29`. The offset is an editable profile field; it is emitted before homing because this firmware applies `state.zoffset` during `G28`. Start/end G-code fields remain editable. The end script disables the hot end, bed and fan, then disables the motors. The default nozzle-preparation script then raises Z, moves to X5/Y5, lowers to Z0.3, draws a short 75 mm purge line to X80 with relative extrusion, retracts slightly and lifts Z again. This is a bed purge line, not a brush wipe. Its coordinates are a provisional front-edge path: verify it lies on the actual usable bed surface and clear of clips/protectors, and edit the nozzle-preparation G-code if your bed geometry differs.

The 290°C hot-end and 110°C bed values are firmware safety ceilings, not recommended material setpoints. The app's material profiles remain within Robo's published PLA/ABS guidance. The firmware repository does not establish the protector's actual usable XY footprint.

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

For this installed firmware, the profile default is 1.0 mm because `CFG_ROBOR1PLUS.md` explicitly specifies that value. This is distinct from Robo support pages describing offsets for other firmware/control workflows. The offset remains editable and must be calibrated against the actual bed/protector and first layer; do not assume that the protector thickness should simply be added to or subtracted from the firmware offset.

## In-app warning rules

The app warns when PLA or ABS temperatures fall outside Robo's published material guidance, ABS part cooling is high, PLA cooling is unusually low, layer height exceeds 0.30 mm, print speed exceeds the app's conservative 60 mm/s starting range, initial-layer speed is high, or the quality/speed control is pushed strongly toward speed.

These are advisory warnings. Filament manufacturers' instructions and the condition/calibration of the actual printer take precedence.

## Before enabling production G-code

Confirm from the specific printer/firmware configuration:

- Actual usable printable XY footprint and its position relative to the home origin (the firmware axis travel is X 240, Y 254, Z 240 mm; its probe grid is X 15–205 and Y 20–230 mm)
- Nozzle diameter and filament diameter
- Heated-bed availability and temperature limits
- Hotend temperature limits
- Maximum feed rates, acceleration and jerk settings
- Firmware flavor and supported G-code commands
- Start G-code, end G-code, homing and bed-leveling behavior (the preset uses G28 followed by G29, as documented for R1+)
- Whether the printer has been modified (hot end, fans, extruder, bed, firmware)

Do not assume all R1 Plus machines have identical firmware or modifications. Keep machine limits and start/end G-code editable per printer profile.

## Sources

- ctkjose, [R1PLUSFIRMWARE](https://github.com/ctkjose/R1PLUSFIRMWARE) — installed firmware source and R1+ configuration
- Marlin Firmware, [Robo3D R1+ machine reference](https://marlinfw.org/docs/appendix/printers/robo3d.html)
- MatterHackers, [Using firmware leveling with Robo R1/R1+](https://help.matterhackers.com/article/62-i-updated-mattercontrol-and-now-i-cant-see-software-print-leveling-in-the-options-menu)
- Robo Help Center, [Adjusting temperatures](https://help.robo3d.com/hc/en-us/articles/115001254531-Adjusting-temperatures)
- Robo Help Center, [Z-axis endstop and R1+ Z-offset guidance](https://help.robo3d.com/hc/en-us/articles/115000724052-Z-Axis-endstop-fix)
- Robo Help Center, [Troubleshooting](https://support-site.readthedocs.io/en/master/R1Plus/troubleshooting/)
- Robo Help Center, [Speed settings](https://help.robo3d.com/hc/en-us/articles/115000716631-Speed)
- ROBO 3D, [FAQ](https://robo3d.com/pages/learn-faq)
- ROBO 3D R1 Plus manual, [manual copy](https://manuals.plus/m/87abab7905bbe0be626af8ae95557d75cd053bb90dffe219323b296514acca72_optim.pdf)
- B&H, [R1 Plus product specifications](https://www.bhphotovideo.com/c/product/1195134-REG/robo_3d_a1_0002_000_r1_plus_3d_printer.html/)
