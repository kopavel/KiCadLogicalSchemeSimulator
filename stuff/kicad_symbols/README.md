## KiCad Symbols

For building the schema simulation net, the project utilizes map files with the extension `.sym_map`, passed through the `-m` parameter.  
This parameter can be passed multiple times for multiple files.  

[//]: # (FixMe point here about XSD file and about sym_param file)
The project contains an example of a library of commonly used symbols in the [stuff/kicad_symbols/chip.kicad_sym](chip.kicad_sym).
The map file for it can be found in [stuff/kicad_symbols/chip.sym_map](chip.sym_map).  
An incomplete KiCad symbols library mapping can be found in [stuff/kicad_symbols/kicad.sym_map](kicad.sym_map). It is updated with new mappings as the project
progresses.  
It is straightforward to adopt your preferred library for use with this simulator using one of the possible mapping solutions.  
Sometimes a combination of both approaches works best. For example, a ROM schema part has size and file name attributes. Size might be mapped for the symbol in the
map
file, but the file path is configured in the symbol instance on the schema itself. Since you can have many ROMs with individual files for each, the mapping file
might not be helpful in all cases.

Refer to the [information](../../schemaParts/README.md) about supported schema parts and their parameters.

### XML Map File Format

The XML map file format is quite simple. Each symbol contains its own map record in the following format:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<symbolMap xmlns="pko.kicadLogicalSimulator.map">
	<lib name="4xxx">
		<symbol name="4001" class="OrGate" param="size=2;reverse">
			<unit pinMap="1=IN0;2=IN1;3=OUT"/>
			<unit pinMap="5=IN0;6=IN1;4=OUT"/>
			<unit pinMap="8=IN0;9=IN1;10=OUT"/>
			<unit pinMap="12=IN0;13=IN1;11=OUT"/>
		</symbol>
	</lib>
</symbolMap>
```

See the complete example in [kicad.sym_map](kicad.sym_map).

- The `lib` tag aggregates symbols from one symbol library, where the `name` itself corresponds to the symbol library name.
- The `symbol` tag describes one symbol from the library, where the `name` attribute corresponds to the symbol name from a specific symbol library.
- The `class` the Java class name used for schema part behavior simulation.
- The `param` additional parameters for class instantiation.
- The `unit` optional tag describes pin mapping for multi-unit symbols
