# Simulator parameters

## CLI

Usage:  [-r] [-do] [-rd] [-md=<mapFileDir>] [-od=\<optimisedDir>] [-m=\<mapFiles>]... [-ro=\<recursiveOuts>]... \<netFilePath\>

### Recursive events

By default, Simulator detect recursive events and show warning about it in LOG/Console.
This has little overhead, comparing with full recursive support, and give compatibility to tune simulator for correct work.  
For that following CLI keys can be used:

- `-r, --recursive`: Enable recursive events processing.  
  Enable recursive events processing for all output pins.
  Has bigger simulation overhead, comparing with detection only logic.
- `-ro, --recursiveOut`: Enable recursive event processing for specific output only.  
  Can be specified multiple time for each output, on which recursive events detected.
- `-rd, --recursive-disabled`: Disable recursive support completely.  
  If schema doesn't generate recursive events (No warning about that from detection logic),
  or after add `-ro` key for all output with recursive events, detection logic can be disabled,
  giving some performance boots.

### Code optimiser

Simulator generate more optimal class versions for specific cases.  
For startup time optimisation those classes are cached in specified by following parameter directory.

- `-od, ----optimisedDir`: Cache directory path for generated optimised classes.  
  If not specified - default is "./optimised" in working folder.

***

- `-do, --disable-optimiser`: Disable code optimiser.  
  User for debug purposes. Not to be specified for regular use.

### Mapping files

- `-m, --mapFile`: Path to KiCad symbol mapping file
  Can be
- `-md, --mapFileDir`: Map file directory path.
  Directory, in which map files searched. Map fieles
- `<netFilePath>`: Path to KiCad NET file

## Schema parameter file

Parameters can be provided, using Schema parameter file. File are searched in working folder, with same name as `netFile` but with `.sym_param` extension.  
Check [XSD](paramFile.xsd) for it.

### Example of parameter file

```xml

<params noRecursive="true" xmlns="pko.kicadLogicalSimulator.param">
	<part id="VCNT_CHAR2" symPartParam="recursive=Q"/>
	<mapFile>kicad</mapFile>
	<mapFile>retro</mapFile>
</params>
```

- `noRecursive`: equivalent of `-rd` CLI key
- `recursive`: equivalent of `-r` CLI key
- `mapFile`: equivalent of `-m` CLI key
- `part`: override Schema part definition for specific ID
    - `ignore`: completely ignore specific schema part.
    - `symPartClass`: points to the Java class name used for schema part behavior simulation.
    - `symPartParam`: additional parameters needed for class instantiation

Parameters and class override priority (first win):

1) Schema parameter file.
2) Symbol fields in schema it self.
3) Mapping file.