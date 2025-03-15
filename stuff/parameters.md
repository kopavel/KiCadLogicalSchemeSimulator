# Simulator parameters

## CLI

Usage:  [-r] [-ro=\<recursiveOuts>]... [-od=\<optimisedDir>] [-do] [-md=<mapFileDir>] [-m=\<mapFiles>]... \<netFilePath\>

### Recursive events

By default, Simulator detect recursive events and show warning about it in LOG/Console.
This has little overhead, comparing with full recursive support, and give compatibility to tune simulator for correct work.  
For that following CLI keys can be used:

* `-r, --recursion`: Configure recursive events processing.
  * `warn` - only report warning and recursive event detection - less overhead, comparing with `all`.
  * `all` - processing recursive events on all output pins (Has bigger simulation overhead, comparing with warning only logic.)
  * `none` - Disable recursive support completely. If schema doesn't generate recursive events (No warning about that from detection logic),
    or after add `-ro` key for all output with recursive events, detection logic can be disabled, giving some performance boots.

* `-ro, --recursiveOut` Enable recursive event processing for specific output only.  
  Can be specified multiple time for each output, on which recursive events detected.

Second option - use Simulation parameter file (see description below).

### Code optimiser

- `-od, ----optimisedDir`: Cache directory path for generated optimised classes.  
  Simulator generate optimised class versions for specific cases.  
  For startup time optimisation those classes cached in specified here directory.  
  If not specified - "./optimised" in working folder used.

- `-do, --disable-optimiser`: Disable code optimiser.  
  Used for debug purposes. Not to be specified for regular use.

### Mapping files

- `-m, --mapFile`: Name/path of KiCad symbol mapping file.  
  Can be specified multiple times for multiple files.
- `-md, --mapFileDir`: Directory for map file search if it can't be found in working directory.
- `<netFilePath>`: Path to KiCad NET file

## Schema parameter file

Parameters can be provided, using Schema parameter file. File searched in working folder, with same name as `netFile` but with `.sym_param` extension.  
Check [XSD](paramFile.xsd) for details.

### Example of parameter file

```xml

<params recursion="all" xmlns="pko.kicadLogicalSimulator.param">
  <part id="VCNT_CHAR2" recursive="Q" param="reverse"/>
	<mapFile>kicad</mapFile>
	<mapFile>retro</mapFile>
</params>
```

- `recursion`: equivalent of `-r` CLI key
- `mapFile`: equivalent of `-m` CLI key
- `part`: override Schema part definition for specific ID
  - `param`: additional parameters needed for class instantiation
  - `recursive`: list of pins with explicitly enabled recursive event handling

Parameters and class definition in Schema parameter file has priority over definition in Mapping file.
