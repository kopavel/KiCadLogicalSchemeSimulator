# Net Filter Design

Net filters transform the parsed KiCad NET before the simulator builds the runtime logical model.

Their purpose is to adapt a physical schematic to the simulator's event-driven binary model and to simplify the schematic before runtime simulation where possible.

The simulator is based on discrete signal events:

```text
LOW
HIGH
HIGH IMPEDANCE
```

It does not model continuous voltage/current propagation over time.

Net filtering is therefore useful when a physical schematic contains elements whose behaviour:

* can be determined from topology before simulation;
* can be represented by a simpler logical topology;
* would otherwise introduce unnecessary runtime objects and propagation;
* cannot be represented directly by the event-driven binary model, but can be approximated for the particular way they are used in the schematic.

---

# Position in the simulation pipeline

Filtering happens after KiCad NET parsing and configuration resolution, but before `SchemaPart` objects and runtime signal connections are constructed.

```text
KiCad schematic
      |
      v
parsed NET
      |
      v
symbol/configuration resolution
      |
      v
NET FILTERING
      |
      v
logical NET
      |
      v
SchemaPart construction
      |
      v
runtime optimisation
      |
      v
simulation
```

A NetFilter therefore operates on the **description of the circuit**, not on an already constructed runtime signal graph.

---

# Why NetFilters exist

There are two main reasons for filtering.

## Pre-runtime optimisation

If a component or connection can be simplified from topology or configuration alone, there is no reason to carry that structure into the runtime event model.

For example:

```text
A ---- closed switch ---- B
```

can become:

```text
A ----------------------- B
```

The runtime model no longer needs:

* the switch schema part;
* its pins;
* its interconnections;
* an additional propagation stage.

Similar simplifications can remove or replace other components whose runtime behaviour is already determined.

This can reduce:

* runtime schema-part count;
* pin/bus count;
* mergers and interconnections;
* signal propagation depth;
* work performed for every later event.

Filtering therefore performs schematic-level optimisation before the runtime optimiser sees the model.

---

## Adapting physical circuitry to the simulator model

The simulator is an **event-driven binary simulator**, not a general analogue simulator.

This does not mean that physically analogue components cannot be represented.

Components such as:

* diodes;
* LEDs;
* passive connections;
* transistors operating in switching mode;

can be implemented with event-driven behaviour when the relevant circuit state can be represented by discrete logical states.

For example, the simulator has passive-pin support that allows diode-like behaviour to react to changes in surrounding logical nets.

Likewise, a transistor used as a switch may sometimes be represented directly or transformed into a simpler logical switching element.

The important limitation is not the physical type of component.

The limitation is whether its relevant behaviour can be expressed as discrete signal events without modelling continuous time-dependent analogue propagation.

---

# Time-dependent analogue behaviour

The simulator does not currently model general continuous-time electrical behaviour such as:

* analogue voltage levels;
* analogue current;
* resistance-dependent voltage division;
* capacitance charging/discharging curves;
* inductive behaviour;
* analogue amplification;
* continuous RC/LC timing behaviour.

Some components involving these properties can still be filtered when their particular use makes their dynamic behaviour irrelevant.

For example, a capacitor used purely for power filtering may potentially be removed from the logical model because it does not contribute meaningful binary runtime
behaviour.

However:

```text
RC
LC
RLC
```

networks whose timing or analogue dynamics affect circuit behaviour cannot simply be represented by the current binary event model.

Such cases require either a future modelling extension or must remain outside the supported simulation scope.

---

# Topology-dependent replacement

A physical component does not necessarily map one-to-one to a particular runtime schema part.

A filter may inspect how it is actually connected and derive a cheaper logical representation.

A transistor, for example, may under a particular topology effectively behave as:

```text
inverter
```

or:

```text
open-collector/open-emitter style switch
```

or, when topology makes the result constant:

```text
direct connection
```

or:

```text
removed branch
```

The transformation is based on the complete local topology, not only on the physical component type.

The goal is to preserve the relevant binary behaviour while avoiding unnecessary runtime modelling of the physical implementation.

---

# NetFilter SPI

A filter implements:

```java
NetFilter
```

with:

```java
boolean doFilter(Export netFile, ParameterResolver parameterResolver);
```

The filter receives:

* the mutable parsed KiCad NET;
* resolved component and pin configuration.

It may modify either where required.

The return value is part of the filtering protocol:

```text
true  -> this invocation actually changed the NET/configuration
false -> nothing was changed
```

A filter must not return `true` merely because it found something interesting.

It must return `true` only when it performed a transformation.

---

# Repeated filtering to a fixed point

All filters are executed repeatedly until a complete pass changes nothing.

Conceptually:

```text
do
    changed = false

    for each NetFilter
        changed |= filter.doFilter(...)

while changed
```

This is necessary because one filter may expose a condition required by another filter.

For example:

```text
+POWER
   |
 DIP switch
   |
 resistor
   |
signal
```

Suppose the DIP switch is configured as permanently closed.

Initially, the resistor is not directly connected to the power rail from the point of view of the resistor filter:

```text
POWER -> DIP -> resistor
```

so the resistor cannot yet be recognised as a simple pull-up/down case.

During one pass, the DIP-switch filter removes the closed switch and merges the nets:

```text
+POWER
   |
 resistor
   |
signal
```

On the next pass, the resistor filter now sees the power connection directly and may replace the resistor with a simpler pull-up/down representation.

Therefore:

```text
pass 1
    filter A changes topology

pass 2
    filter B sees a newly exposed case

pass 3
    another transformation may become possible

...

final pass
    no filter changes anything
```

Filtering finishes only when **every filter returns `false` during the same complete pass**.

---

# Filter convergence

Because filtering is repeated, every filter must converge.

After performing its transformation, it must eventually stop finding additional work in the already transformed structure.

The required behaviour is:

```text
applicable + transformed
    -> true

already transformed / no mutation required
    -> false

not applicable
    -> false
```

Returning `true` without changing anything would prevent the filtering process from ever reaching its fixed point.

---

# Filter ordering

Filters should not rely on a particular `ServiceLoader` execution order.

The repeated-pass design deliberately reduces ordering dependencies.

A filter may rely on this:

> If another filter changes the topology in a way that enables my transformation, I will inspect the resulting NET again on a later pass.

It should not rely on this:

> Another particular filter always executes before me.

This allows independently provided schema-part modules to contribute transformations without requiring a global hand-maintained filter order.

---

# Typical transformations

NetFilter transformations are not limited to a fixed set of operations.

Typical cases include:

## Removing components

```text
A -- permanently conductive element -- B
```

becomes:

```text
A ----------------------------------- B
```

---

## Merging nets

Removing a component may result in multiple physical KiCad nets becoming one logical net.

---

## Replacing components

A physical implementation may be replaced by a simpler logical one.

Examples:

```text
resistor + fixed rail
        ->
logical pull-up/down
```

or:

```text
transistor topology
        ->
logical switching element
```

---

## Rewriting pins

A replacement schema part may expose a different logical interface from the original KiCad symbol.

The filter may therefore rewrite pin names, directions or node information before runtime graph construction.

---

## Adding synthetic components

Logical behaviour may exist implicitly in a physical component or topology.

A filter may add a synthetic logical component even when no standalone KiCad symbol exists for it.

For example:

```text
internal pull-up
```

may become an explicit logical source connected to the corresponding runtime net.

---

# Physical topology vs logical topology

The filtered runtime model is not required to reproduce the KiCad schematic one-to-one.

The physical schematic describes:

```text
what is physically connected
```

The filtered model describes:

```text
what discrete logical behaviour must be simulated
```

For example:

```text
physical topology:

POWER
  |
switch
  |
resistor
  |
transistor
  |
signal
```

may reduce, depending on configuration and topology, to a much smaller logical structure:

```text
logical source
     |
logical switching element
     |
signal
```

The missing physical elements were not forgotten.

Their relevant behaviour was resolved before runtime.

---

# Relationship to runtime optimisation

Net filtering and runtime optimisation solve different problems.

Net filtering operates on schematic topology:

```text
physical NET
    |
    | remove / replace / merge / rewrite
    v
logical NET
```

Runtime optimisation operates later on simulation objects:

```text
SchemaPart graph
    |
    | shortcut / specialise / cut / unroll
    v
optimised runtime graph
```

A transformation known from schematic topology should normally happen as early as possible.

Removing a useless physical abstraction in NetFilter means that the runtime graph never has to construct, connect or optimise it at all.

---

# Helper API

`NetFilter` currently contains helper methods for several recurring transformation patterns, including operations for:

* finding related component pins;
* rewriting related nodes;
* replacing schema-part configuration;
* merging nets;
* adding synthetic parts.

These helpers are **convenience API, not a stable or complete filtering abstraction**.

Their current shape reflects the filtering cases implemented so far and may be changed, extended or replaced as new transformations require different operations.

A new filter does not have to force its logic into an existing helper.

It may:

* use the existing helper as-is;
* extend or improve the helper API;
* add another reusable helper;
* manipulate the parsed NET/configuration directly when that is clearer.

Existing filters should be treated primarily as examples of possible transformations rather than as the complete definition of what a NetFilter is allowed to do.

---

# NetFilter design rules

A NetFilter should:

* operate before runtime graph construction;
* transform the physical schematic into an equivalent event-driven binary model;
* use topology and configuration information to eliminate runtime work where possible;
* preserve the logical behaviour relevant to the simulated circuit;
* not attempt to reproduce continuous-time analogue behaviour using incorrect binary approximations;
* allow physical components to remain when their behaviour is already representable by the normal event model;
* replace or remove them only when topology makes a simpler representation valid;
* return `true` only after an actual transformation;
* converge under repeated execution;
* not depend on filter execution order;
* expect transformations from other filters to expose additional cases on later passes;
* keep the parsed NET and resolved configuration mutually consistent;
* prefer the simplest runtime topology that remains logically equivalent at the simulator's abstraction level.

---

# Summary

Net filtering acts as a schematic-level compilation and optimisation stage.

```text
physical KiCad circuit
        |
        | inspect topology
        | resolve fixed conditions
        | remove unnecessary elements
        | merge known connections
        | replace expensive/unsupported forms
        | add implicit logical behaviour
        v
event-driven binary circuit
        |
        v
runtime simulator
```

The goal is not to convert every physical component into a separate software object.

The goal is to produce the simplest valid event-driven logical model of the circuit before runtime simulation begins.
