# Architecture

KiCad Logical Scheme Simulator is an event-driven binary circuit simulator built around a small simulation core and pluggable schema-part modules.

The simulator operates on discrete signal states:

```text
LOW
HIGH
HIGH IMPEDANCE
```

Physical circuit topology is transformed into the simplest equivalent event-driven logical model before simulation where possible.

The design prioritises:

* fast signal propagation;
* minimal runtime topology;
* simple schema-part APIs;
* topology- and configuration-aware optimisation;
* separation between physical KiCad representation and runtime logical behaviour.

The simulator does not attempt to model general continuous-time analogue propagation. Physical components such as diodes, passive elements and transistors in
switching configurations can still be represented when their relevant behaviour can be expressed through discrete events.

## Developer documentation

* [Signal / wire and bus API](simulator/src/java/pko/KiCadLogicalSchemeSimulator/api/DESIGN_API.md)
* [Schema-part design, SPI, configuration, UI and testing](schemaParts/DESIGN_SchemaPart.md)
* [NET filtering and schematic-level optimisation](simulator/src/java/pko/KiCadLogicalSchemeSimulator/api/DESIGN_NetFIltering.md)

## Simulator module

The `simulator` module provides the simulation infrastructure and public APIs used by schema parts.

Responsibilities:

* Parse KiCad NET data and resolve symbol mappings and component configuration.
* Run pluggable NET filters before runtime graph construction.
* Discover schema-part implementations through `SchemaPartSpi`.
* Build the runtime schema-part object graph.
* Interconnect pins and buses, including passive connections and signal mergers.
* Run the event-driven signal model.
* Optimise schema-part implementations using topology and configuration known at runtime.
* Optimise and shortcut runtime source/destination interconnections.
* Stabilise the complete model before normal simulation begins.
* Provide UI and monitoring infrastructure for interactive schema parts.

## Schema-part modules

The `schemaParts` tree contains pluggable component implementations built on top of the simulator API.

A schema-part module may provide:

* one or more `SchemaPartSpi` implementations;
* component-specific pin/bus event logic;
* runtime source optimisation;
* interactive or monitoring UI;
* `NetFilter` implementations required to transform physical KiCad topology into a suitable logical model;
* Spock tests for generic and optimised component behaviour.

Schema parts are discovered through Java service loading rather than direct dependencies from the simulator core.

## Simulation lifecycle

At a high level, a simulation is built in the following stages:

```text
KiCad NET
    |
    v
resolve symbol mappings and configuration
    |
    v
run NetFilters to a fixed point
    |
    v
construct SchemaParts
    |
    v
group and interconnect pins/buses
    |
    v
build mergers and runtime signal graph
    |
    v
specialise/optimise schema parts and interconnections
    |
    v
stabilise the complete model
    |
    v
event-driven simulation
```

### NET filtering

Before runtime objects are built, all discovered `NetFilter` implementations are repeatedly applied until a complete pass makes no changes.

Filtering can:

* remove components whose behaviour is already determined;
* merge nets through known conductive paths;
* replace physical components with simpler logical equivalents;
* rewrite logical pin roles;
* add implicit/synthetic logical components.

Repeated execution is necessary because one transformation may expose a topology that another filter can simplify on a later pass.

This stage serves both compatibility and performance: it allows physical KiCad circuitry to be adapted to the simulator's event-driven binary model and prevents
unnecessary physical abstractions from entering the runtime graph.

See [NET Filter Design](simulator/src/java/pko/KiCadLogicalSchemeSimulator/api/DESIGN_NetFIltering.md).

## Signal event model

Signal propagation is the primary runtime hot path.

A propagated signal event represents an **actual state change**.

If a downstream model item receives:

```java
setHi()

setLo()

setState(...)

setHiImpedance()
```

it may assume that the signal changed.

Propagating an unchanged state is therefore forbidden.

When a newly calculated state may equal the existing state, propagation must stop before entering the graph:

```text
calculate new state
        |
        v
change required?
    |        |
   no       yes
    |        |
   STOP      v
          propagate
```

When component semantics already guarantee that the state changes, the comparison itself should be omitted.

This contract allows state-change detection to happen once rather than being repeated at every node in the propagation path.

See [Signal API Design](simulator/src/java/pko/KiCadLogicalSchemeSimulator/api/DESIGN_API.md).

## Performance-oriented design

The simulator applies optimisation at several different stages.

### 1. Schematic-level optimisation

`NetFilter` removes or replaces unnecessary physical topology before runtime objects exist.

```text
physical KiCad NET
        |
        v
minimal logical NET
```

Anything eliminated here does not need to be constructed, interconnected, specialised or processed during simulation.

### 2. Schema-part source specialisation

Runtime-known configuration and topology can be used to generate specialised Java implementations.

The source optimiser can:

* cut unreachable branches and code;
* bind runtime-known values as constants;
* unroll fixed-size loops;
* generate specialised component variants.

Conceptually:

```text
generic schema-part source
        |
        | topology/configuration
        v
specialised Java class
        |
        v
JVM JIT
```

The optimiser therefore performs partial evaluation before the JVM sees the final hot code.

### 3. Runtime graph optimisation

After actual source/destination topology is known, runtime objects can also be specialised or replaced.

Examples include:

* direct source-to-destination shortcutting;
* specialised zero/one/multiple-destination implementations;
* merger optimisation;
* removal of unnecessary intermediate state storage and indirection.

Because runtime objects may be replaced during this stage, schema parts must follow the rebinding contract described
in [Schema Part Design](schemaParts/DESIGN_SchemaPart.md).

### 4. JVM optimisation

The resulting Java code and object graph are finally optimised by the JVM JIT compiler.

The previous optimisation stages intentionally reduce abstraction, branching and indirection before JIT compilation so the JVM receives a smaller and more concrete
hot path.

## State storage

Signal state does not necessarily need to be duplicated in every object along a propagation path.

Optimisation may remove redundant intermediate state storage where component logic does not depend on it.

Hot-path schema-part logic that requires a particular input state should use the local direct `state` field and must preserve that state during source optimisation.

Generic state lookup through the source chain remains available for monitoring, diagnostics and other non-hot-path use.

## Stabilisation

After the graph is constructed and optimised, the simulator performs a stabilisation phase before normal simulation begins.

Initial output states are propagated through the completed graph, merger/retry conditions are resolved, and schema parts are reset as required until the constructed
model reaches its normal starting state.

Only after stabilisation is the model treated as running simulation state.

## UI separation

Interactive schema parts may expose visual components and monitoring information, but UI processing is intentionally separated from the main signal propagation path.

Fast-changing simulation state should normally be sampled or aggregated for presentation rather than causing expensive UI work for every propagated event.

User interaction may initiate schema-part operations, but resulting signal changes still follow the normal signal event contract.

## Design anti-patterns

* Propagating an unchanged signal. Downstream logic is allowed to assume that every event represents a real change.
* Re-checking a state change repeatedly along the propagation path when it was already guaranteed upstream.
* Treating method calls and abstraction layers as free in hot event-processing code. Prefer direct object and field access where appropriate.
* Keeping physical topology in the runtime model when it can be safely resolved by NET filtering beforehand.
* Assuming constructor-created runtime pin/output objects will necessarily survive graph optimisation.
* Removing local input state during optimisation when any hot-path component logic directly reads that state.
* Performing UI rendering or refresh work for every high-frequency simulation event.
