# Architecture

KiCad Logical Scheme Simulator is built around a small event-driven simulation core and a set of pluggable schema-part implementations. The design prioritises fast signal propagation and a simple component API.

## Simulator module

The `simulator` module provides the simulation infrastructure and public APIs used by schema parts.

Responsibilities:

- Publish the [wire and bus API](simulator/src/java/pko/KiCadLogicalSchemeSimulator/api/DESIGN_API.md) used by circuit components.
- Publish the UI API used by interactive schema parts.
- Run the event-driven simulation model. An event is a wire or bus state change initiated by a schema part and propagated through the object graph.
- Implement signal merging and splitting between multiple inputs and outputs.
- Parse KiCad schematic/netlist data and build the runtime schema-part object graph.
- Perform runtime source optimisation of schema parts when configuration and topology make conditions known. The optimiser can:
  - cut unreachable code and branches;
  - bind runtime values as constants;
  - unroll cycles/loops when their size is known.
- Perform runtime interconnection optimisation and shortcutting between source and destination objects to reduce event-propagation overhead.
- Stabilise the constructed model before simulation begins.

### Simulation lifecycle

At a high level, the simulator performs the following stages:

1. Parse the KiCad schematic/netlist.
2. Build and interconnect schema-part objects.
3. Optimise schema-part source using known runtime conditions.
4. Optimise/shortcut runtime object interconnections.
5. Stabilise the complete model.
6. Start event-driven simulation.

## Schema parts module

The `schemaParts` module contains concrete component implementations built on top of the simulator API.

Responsibilities:

- Implement the logic of specific schema parts using the simulator wire/bus API.
- Implement schema-part UI for interactive components.
- Implement KiCad NET-file preprocessing/filtering required by specific schema parts or component families.

## Performance-oriented design

Signal propagation is the primary hot path. The architecture intentionally allows implementation choices that would be unusual in less performance-sensitive application code.

The optimiser performs partial evaluation before the JVM JIT compiler sees the hot code. Runtime-known topology and configuration are used to generate specialised Java classes with unnecessary branches removed, values bound as constants, and fixed-size loops unrolled. This gives the JVM a smaller and more concrete control-flow graph to optimise further.

Runtime object interconnections are also optimised after the schema graph is known, reducing unnecessary intermediate propagation and indirection where possible.

## Anti-patterns

- Treating method calls as free in hot paths. Every additional call can add dispatch, stack/inlining pressure, aliasing uncertainty, or prevent further JVM optimisation. Avoid getters/setters and unnecessary wrapper abstractions in event-propagation code; prefer direct object and field references where safe and practical.
