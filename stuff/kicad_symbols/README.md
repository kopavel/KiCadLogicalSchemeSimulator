## KiCad Symbols

For building the schema simulation model, the project utilizes a map file named `SymbolsDescription.xml` located in the 'distro' directory. Another approach is
declaring custom fields `SymPartClass` and/or `SymPartParam` directly in KiCad symbols.

- `SymPartClass` points to the Java class name used for schema part behavior simulation.
- `SymPartParam` is for additional parameters needed for class instantiation. Parameters should be provided in a semicolon-separated list,
  like `parameterName1=value1;parameterName2=value2;ParameterName3=Value3`. Some schema part Java classes require certain parameters to be mandatory.

The project contains an example of a library of commonly used symbols in the 'stuff/kicad_symbols' directory, and the project map file is already configured for
those symbols. However, it's straightforward to adopt your preferred library for use with this simulator using one of the possible mapping solutions. Sometimes a
combination of both approaches works best. For example, a ROM schema part has size and file name attributes; where size might be mapped for the symbol in the map
file, but
the file path is configured in the symbol instance on the schema itself. Since you can have many ROMs with individual files for each, the mapping file might not be
helpful in all cases.

Refer to the [information](..%2F..%2FschemaParts%2FREADME.md) about supported schema parts and their parameters.

### XML Map File Format

The XML map file format is very simple. Each symbol contains its own map record in the following format:

```xml

<symbolMap>
  <lib name="chip">
    <symbol name="3OR" symPartClass="OrGate" symPartParam="size=3"/>
  </lib>
</symbolMap>
```

see complete example in [SymbolsDescription.xml](SymbolsDescription.xml)

- `lib` tag agregate symbols from one symbol library, where attribute `name` coresponde to the symbol library name.
- `symbol` tag describe one symbol from library, where attribute name corresponde to Symbol name from a specific symbol library.
- `symPartClass` attribute is the Java class name used for schema part behavior simulation.
- `symPartParam` attribute is additional parameters for class instantiation.

Fields from the map file and declared in symbols directly are merged.
Parameters, declared in symbol directly have higher priority. (because of 'last-win' approach)
