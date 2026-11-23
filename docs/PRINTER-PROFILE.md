# Robo 3D R1 Plus printer profile

The initial profile name is set to **Robo 3D R1 Plus**, but the dimensional defaults in the scaffold are placeholders and must be verified before slicing.

Before enabling real G-code output, confirm from the specific printer/firmware configuration:

- Usable X/Y/Z build volume and origin convention
- Nozzle diameter and filament diameter
- Heated-bed availability and temperature limits
- Hotend temperature limits
- Maximum feed rates, acceleration and jerk settings
- Firmware flavor and supported G-code commands
- Start G-code, end G-code, homing and bed-leveling behavior

Do not assume all R1 Plus machines have identical firmware or modifications. Keep machine limits and start/end G-code editable per profile.
