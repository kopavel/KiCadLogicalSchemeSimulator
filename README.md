# KiCad logical scheme simulator

[KiCad](https://www.kicad.org) plugin for interactive logical scheme simulation.
Supports two usage methods: directly from KiCad or standalone, using a schema file exported from KiCad in the "PCB Netlist"(`.net`) file format.

Focuses on performance and simplicity of the net schema part API, allowing easy implementation of custom schema part behaviors for simulation.  
Tend to provide out-of-the-box experience. For that has predefined mapping for several KiCad libraries, like 4xxx, some 74xx and others.

More info about [how to configure KiCad symbols mapping](stuff/kicad_symbols/README.md) for the simulator and [supported schema part list](schemaParts/README.md).

Be sure helping this project by implementing new components and/or extend mapping for existing implementation and share result with comunity.

## Building

To build the project, execute the Gradle assemble task:

```
./gradlew clean assemble
```

If the build is successful, you will find the output in the 'distro' folder.

## Standalone Start

When the schema are prepared and exported from KiCad in the "PCB Netlist" (`.net`) file format, you can start the project.  
Use the appropriate script based on your operating system from the 'distro' directory.

For Unix-like systems:

```bash
./run.sh <path>/<to_my_project>/mySchema.net
```

For Windows systems:

```bat
run.bat <path>/<to_my_project>/mySchema.net
```

These scripts initiate the simulator with the specified `.net` file as the first parameter.

## KiCad Plugin Mode

KiCad currently supports plugins only in the PCB editor, not in the Schematic Editor (Eeschema). However, a workaround exists using BOM (Bill Of Material) scripts,
which are custom [Python](https://www.python.org) scripts that receive the path to an XML variant of the "PCB Netlist" as the first parameter. This mechanism can be
used to launch the simulator directly from the schematic editor. Add the custom BOM generation script `simulate.py` from the 'distro' directory. Then, when
you run "BOM generation" using that script, the simulator will be started instantly with the schema currently open in the editor.  
On KiCad prior version 8 BOM generations are accessed directly in the toolbar. In version 8 BOM generation has been changed and generation from a python script now
located under the menu Tools->"Generate Legacy Bill of Materials...". That can be assigned to your preferred hotkey for quick access.

## Usage

After launching simulator builds the simulation net and displays all “interactive” schema parts, such as LEDs, Displays or oscillators on the desk.  
Parts can be arranged freely, and the layout saved near the Netlist file in a file with the `.sym_layout` extension at simulator close.  
See also simulator [parameter description](stuff/parameters.md) for fine tune simulation process.

Check [component list](schemaParts/README.md) for a detailed description of supported schema components.

For schema debugging purposes, an [oscilloscope](schemaParts/oscillator/OSCILLOSCOPE.md) from the [oscillator](schemaParts/oscillator/README.md) component can be
used.

Alternatively, schema part monitoring is available from the main interface menu.  
After selecting a target schema part, its pin representation appears on the screen, showing the current signal on each pin.  
Optionally, for certain schema parts, additional information about internal status is displayed.  
For example, for the Z80 CPU:

![Z80 CPU Image](img.png)

## Examples

Here are some schema Netlist examples:

- [Counters](stuff/examples/counters/README.md) - Simple chain of counters and LEDs.  
  for start use corresponding gradle task:

```
./gradlew counters
```

- [Clock](stuff/examples/clock/README.md) - Simple 24h clock.  
  for start use corresponding gradle task:

```
./gradlew clock

```

- [Z80](stuff/examples/z80/README.md) - Schema based on the Zilog Z80.

```
./gradlew z80
```

- [TRS80](stuff/examples/trs80/README.md) - Stripped TRS80-I schema.

```
./gradlew trs80
```

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). You can find the full text of the license in the [LICENSE](LICENSE) file.

Project use sources from other projects:

- https://github.com/sethm/symon for Mos6502 CPU core logic;
- https://github.com/codesqueak/Z80Processor for Z80 CPU core logic;
