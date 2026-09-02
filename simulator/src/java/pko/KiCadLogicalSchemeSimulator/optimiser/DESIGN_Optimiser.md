# Runtime Optimiser Design

KiCad Logical Scheme Simulator performs runtime optimisation after the logical circuit topology is known.

The optimiser uses information that is unavailable when the generic schema-part classes are compiled:

* actual source/destination topology;
* number and types of destinations;
* tri-state requirements;
* recursion configuration;
* component parameters;
* fixed masks, offsets and sizes;
* other schema-specific conditions known during model construction.

This information is used to remove generic behaviour that is unnecessary for a particular runtime instance.

The goal is not merely to reduce object count.

The optimiser attempts to present the JVM with a smaller, more concrete hot path containing fewer:

* branches;
* loops;
* virtual calls;
* array accesses;
* state copies;
* runtime configuration checks;
* intermediate objects.

Conceptually:

```text
generic Java implementation
          |
          | actual runtime topology/configuration
          v
specialised Java implementation
          |
          | runtime compilation
          v
specialised bytecode
          |
          | JVM JIT
          v
machine code
```

The custom optimiser therefore performs partial evaluation **before** the JVM JIT compiler sees the final hot implementation.

---

# Optimisation layers

Runtime optimisation consists of two related but distinct mechanisms:

```text
runtime topology
      |
      +----> graph/object optimisation
      |
      +----> source specialisation
```

They are often used together but solve different problems.

## Graph/object optimisation

Graph optimisation changes the actual runtime object path.

Examples include:

* replacing unused outputs with no-connect implementations;
* shortcutting a one-destination output directly to its destination;
* replacing generic fan-out with specialised implementations;
* optimising merger inputs and adapters;
* avoiding redundant intermediate signal state.

## Source specialisation

`ClassOptimiser` generates a specialised Java subclass for a particular runtime variant.

It can:

* remove lines;
* remove blocks/branches;
* replace runtime values with literals;
* unroll known-size loops;
* convert array destinations into individual final object references.

These two stages combine to simplify both:

```text
object topology
```

and:

```text
code executed for each event
```

---

# Optimisation entry point

Runtime model items expose:

```java
getOptimised(ModelItem<?> source)
```

A model item may return:

```text
itself
```

when no replacement is required, or:

```text
another object
```

representing its optimised runtime implementation.

The `source` argument describes the upstream runtime object from which the item receives its signal.

This information allows an item to specialise itself based on its actual position in the propagation graph.

The default `ModelItem` implementation simply stores the source and returns itself.

Specialised pins, buses, mergers and schema-part inputs may override this behaviour.

---

# Topology optimisation

An output can often be specialised purely from its number of destinations.

For example, a wire output conceptually has three important cases:

```text
0 destinations
1 destination
N destinations
```

## No destinations

An unconnected output does not need a normal propagation implementation.

It can be replaced by a no-connect runtime object.

```text
OutPin
   |
   X
```

becomes effectively:

```text
NC
```

The schema part can continue to operate against an output object without carrying a normal propagation graph behind it.

---

## One destination

A normal generic connection would conceptually be:

```text
source OutPin
      |
      v
destination InPin
```

When there is exactly one destination, the intermediate output propagation object can often be shortcut.

The destination receives the source state/context and may itself be optimised against the original source.

Conceptually:

```text
before:

SchemaPart
   |
 OutPin
   |
 InPin
   |
next logic


after:

SchemaPart
   |
optimised InPin
   |
next logic
```

This removes an unnecessary propagation level.

---

## Multiple destinations

When fan-out is required, the output itself remains necessary.

However, the actual destination count and types are now known.

A generic loop such as:

```java
for(Pin destination :destinations){
        destination.

setHi();
}
```

can therefore be replaced with a specialised implementation containing direct references to the concrete destinations.

This is where source specialisation becomes particularly useful.

---

# Edge-specific fan-out

Runtime topology is also used to avoid propagating transitions to inputs that cannot use them.

For example:

* `RaisingEdgePin` does not require LOW-event component logic;
* `FallingEdgePin` does not require HIGH-event component logic.

A generic output can therefore split destinations according to the events they actually consume.

Conceptually:

```text
all destinations
      |
      +---- HIGH destinations
      |
      +---- LOW destinations
      |
      +---- Hi-Z destinations
```

The specialised output propagates each event only to the required destination set.

This optimisation happens after the actual destination types are known.

---

# State ownership and source chains

Graph optimisation may remove redundant local state storage.

A generic chain might contain:

```text
Out.state
   |
Intermediate.state
   |
In.state
```

but storing the same state at every level is often unnecessary.

After shortcutting:

```text
Out.state
   |
Intermediate
   |
In
```

or even:

```text
Out.state
   |
optimised In
```

may be sufficient.

`ModelItem.source` records the upstream owner/path used when effective state must still be obtained.

Generic methods such as:

```java
getState()

isHiImpedance()
```

can follow this source relationship.

This is useful for:

* monitoring;
* oscilloscope/debug views;
* `toString()`;
* diagnostics;
* other non-hot-path introspection.

It is intentionally not a replacement for local state required by hot component logic.

---

# Local state safety

Removing local input state is valid only when no hot-path component logic depends on that local field.

For example:

```java
class Chip {
    Pin enabled;
    Pin clock;
}
```

if clock processing uses:

```java
if(enabled.state){
        ...
        }
```

then `enabled` must continue maintaining its local `state`.

It is not enough that `enabled.setHi()` itself does not read the field.

Any event-processing logic in the schema part may depend on it.

Therefore an optimiser may remove:

```java
state =true;
state =false;
```

from an intermediate input only when local state is not required anywhere by the component's hot-path logic.

Using `getState()` through a source chain instead would preserve semantic access but introduce extra indirection and method calls into the event path, defeating the
purpose of this optimisation.

---

# Signal event contract

Runtime optimisation must preserve the Signal API contract:

> Every propagated event represents a real state change.

Optimisation must never introduce repeated propagation of an unchanged signal.

Likewise, it should not reintroduce state comparisons that are unnecessary because the component logic already guarantees a transition.

The optimiser is allowed to remove checks when their result is known from topology or component semantics.

It is not allowed to weaken the event invariant.

Generic and optimised implementations must therefore remain equivalent in externally observable signal behaviour.

---

# SchemaPart replacement

Optimisation can replace both outputs and custom inputs with different runtime objects.

`SchemaPart` maintains its registered input/output maps accordingly.

## Outputs

During:

```java
optimiseOuts()
```

registered outputs are passed through:

```java
getOptimised(null)
```

and replaced when a different object is returned.

This is why schema-part code must obtain final output references in:

```java
initOuts()
```

rather than assuming that the output object created in the constructor remains the runtime object.

Conceptually:

```text
constructor

out = original OutPin
        |
        v
graph optimisation
        |
        v
out = specialised runtime object
        |
        v
initOuts()
        |
        v
schema part binds final reference
```

---

## Custom inputs

A custom input may also replace itself from its own:

```java
getOptimised(source)
```

implementation.

If the schema part keeps additional component-owned references to that input, those references must be updated as well.

A typical implementation calls:

```java
parent.replaceIn(this,build);
```

after building the specialised input.

If the component maintains another structure such as:

```java
Map<String, MyInput>
```

its `SchemaPart.replaceIn(...)` override must update that structure too.

---

# `ClassOptimiser`

`ClassOptimiser<T>` performs source-to-source specialisation.

Typical usage:

```java
ClassOptimiser<MyInput> optimiser = new ClassOptimiser<>(this).bind("mask", mask).cut("genericBranch");

if(someRuntimeCondition){
        optimiser.

cut("otherBranch");
}
MyInput build = optimiser.build();
```

The generic class contains optimiser directives inside specially formatted comments.

At runtime, `ClassOptimiser`:

1. loads the generic Java source;
2. interprets optimiser directives;
3. removes selected source lines/blocks;
4. substitutes bound values;
5. unrolls selected loops;
6. generates a new `final` subclass;
7. compiles it with the Java compiler;
8. loads the generated class;
9. constructs an instance using the original runtime object.

The resulting specialised class inherits untouched behaviour from the generic class and overrides only the methods whose source had to be specialised.

---

# Generated classes

Generated variants are placed under an `.optimised` package derived from the source package.

Conceptually:

```text
pko....components.AND.AndGateIn
```

becomes a generated class similar to:

```text
pko....optimised.components.AND.AndGateIn_<variant>
```

The generated class is:

```java
final
```

and extends the original implementation.

Variant names encode the active `cut`, `bind` and `unroll` configuration.

This gives each distinct specialisation a stable class identity.

Generated classes are cached and reused when the same runtime specialisation is required again.

Generated Java source and compiled classes may also be stored under the configured optimisation directory.

---

# Generic fallback

The generic implementation remains the source of truth.

If no optimisation operation was requested, or the optimiser is disabled:

```text
generic instance
```

is returned.

If dynamic compilation fails, the current implementation can also fall back to the generic instance unless optimiser compilation errors are configured as fatal.

This means generic component code must always remain correct independently of the optimiser.

Optimisation is not allowed to be required for functional correctness.

---

# Source optimiser directives

Optimiser directives use comments of the form:

```java
/*Optimiser ...*/
```

These are not ordinary documentation comments.

They form a small source-transformation DSL interpreted by `ClassOptimiser`.

Moving, deleting or changing such comments may change generated runtime code.

---

# `constructor`

An optimisable class provides a special copy-style constructor marked with:

```java
/*Optimiser constructor*/
```

For example:

```java
/*Optimiser constructor*/
public MyInput(MyInput oldInput, String variantId) {
    super(oldInput, variantId);

    parent = oldInput.parent;
    mask = oldInput.mask;
    out = oldInput.out;
}
```

The generated subclass receives its own constructor based on this definition.

The constructor copies runtime state/references from the original generic object into the specialised instance.

The generated variant must have the constructor shape expected by `ClassOptimiser`:

```text
(old instance, variantId)
```

The optimiser expects one resulting constructor for instantiation.

---

# `cut(...)`

A runtime optimisation condition activates a named cut:

```java
optimiser.cut("setter");
```

Source directives associate lines or blocks with that cut name.

`cut` means:

> this source region is unnecessary for this runtime variant.

Typical uses include removing:

* state setters that are no longer locally required;
* impossible tri-state paths;
* unused recursion handling;
* configuration branches whose outcome is already known;
* generic output handling replaced by direct topology-specific behaviour.

---

# `line`

A single source line can be associated with a cut:

```java
/*Optimiser line setter*/
state = true;
```

When:

```java
optimiser.cut("setter");
```

is active, the marked source line is removed from the generated implementation.

This is useful for small hot-path operations whose necessity depends on runtime topology.

For example, local input state storage can be removed when state ownership remains upstream and no component logic reads the local field.

The semantic safety of such removal is the responsibility of the optimiser implementation.

---

# `block` / `blockEnd`

A larger source region is marked using:

```java
/*Optimiser block someCase*/
...
/*Optimiser blockEnd someCase*/
```

When:

```java
optimiser.cut("someCase");
```

is active, the marked block is removed.

This makes it possible to specialise generic branching logic.

For example:

```java
/*Optimiser block triState*/
if (hiImpedance) {
    ...
}
/*Optimiser blockEnd triState*/
```

can disappear completely when runtime topology proves that tri-state propagation is impossible or unnecessary.

Unlike relying on the JVM to discover that a branch is constant later, the generated Java source does not contain the branch at all.

---

# Composite cut conditions

Cut names may be combined:

```text
A&B
```

A source region marked with a composite name is cut only when all referenced cuts are active.

Conceptually:

```text
/*Optimiser line A&B*/
```

means:

```text
remove this line when cut("A") AND cut("B") are both active
```

This allows the generic source to express transformations that depend on combinations of topology conditions without adding another runtime branch.

---

# `bind(...)`

Runtime-known values can be substituted directly into generated source.

For example:

```java
new ClassOptimiser<>(this)
        .

bind("mask",mask);
```

with source:

```java
/*Optimiser bind mask*/
state |= mask;
```

can generate code containing the actual numeric value instead of a field lookup.

Conceptually:

```java
state |=mask;
```

becomes:

```java
state |=4;
```

for a variant where `mask == 4`.

Binding is useful for values such as:

* bit masks;
* offsets;
* fixed configuration booleans;
* fixed sizes;
* constants derived during graph construction.

This allows the Java compiler and JVM JIT to perform further constant folding and simplification.

---

# Bind pattern

A directive may explicitly identify the source token/pattern associated with a logical bind ID:

```text
/*Optimiser bind id:sourceToken*/
```

The runtime side still supplies:

```java
optimiser.bind("id",replacement);
```

This allows several source expressions to be controlled through a stable optimiser identifier even when the literal source token has another name.

---

# `unroll(...)`

Known-size destination loops can be completely unrolled.

Runtime code requests unrolling with:

```java
optimiser.unroll(size);
```

or for independently named loops:

```java
optimiser.unroll("h",highCount);
optimiser.

unroll("l",lowCount);
```

The source constructor describes the iterable field and loop variable:

```java
/*Optimiser constructor unroll destination:destinations*/
```

or with a specific unroll ID:

```java
/*Optimiser constructor
  unroll low:toLow:l
  unroll high:toHi:h*/
```

The optimiser uses this information to replace an array/loop with individual final destination references.

Conceptually:

```java
for(Pin destination :destinations){
        destination.

setHi();
}
```

with three runtime destinations becomes structurally similar to:

```java
destination0.setHi();
destination1.

setHi();
destination2.

setHi();
```

where:

```text
destination0
destination1
destination2
```

are generated final fields.

This removes:

* loop control;
* array indexing;
* per-iteration array loads;
* uncertainty about destination count.

It also gives the JVM explicit object references that may be easier to inline and optimise.

---

# Multiple unroll groups

A class may contain several independently specialised destination groups.

For example an output pin can maintain separate lists for:

```text
HIGH
LOW
Hi-Z
```

and request different unroll sizes:

```java
.unroll("h",toHi.length)
.

unroll("l",toLow.length)
.

unroll("i",toImp.length)
```

A zero-sized or unused path does not need to generate runtime loop code.

This is particularly useful for event-specific fan-out.

---

# Unroll index placeholders

Inside an unrolled source block the optimiser supports placeholders associated with the generated iteration:

```text
unrollIndex
unrollIndexPower
```

They represent respectively:

```text
current zero-based unroll index
2 ^ current index
```

and allow a generic loop body to contain iteration-dependent constants that become literals in generated source.

These placeholders are part of the source-transformation DSL rather than normal runtime variables.

---

# `getOptimised()` in generated classes

Once a specialised class has been generated, it should not recursively specialise itself again.

Generated implementations therefore replace their optimisation entry point with behaviour equivalent to:

```java
return this;
```

The object is already the runtime-specialised variant for its topology.

---

# Assertions and generated variants

The generated implementation can differ depending on whether Java assertions are enabled.

When assertions are disabled, assertion-only source can be removed from generated hot-path code.

Assertion-enabled and assertion-disabled variants receive different runtime class identities so they are not accidentally reused as the same optimisation variant.

Assertions must not contain logic required for normal component correctness.

---

# Example: custom input specialisation

A generic AND input may contain:

```java
public void setHi() {
    /*Optimiser line setter*/
    state = true;

    /*Optimiser bind mask*/
    ...

    /*Optimiser block reverse*/
    ...
    /*Optimiser blockEnd reverse*/
}
```

Its runtime optimisation may know:

* the exact bit mask;
* whether the gate is reversed;
* whether open-collector behaviour is enabled;
* whether this input needs its own local state.

The optimiser can therefore:

```java
.bind("mask",mask)
.

cut("unused polarity branch")
.

cut("unused output mode")
.

cut("setter")
```

when each transformation is valid.

Instead of asking the JIT to rediscover component configuration on every specialised instance, the generated Java method contains only the path that can execute for
that concrete input.

---

# Example: output fan-out specialisation

A generic output must support:

* arbitrary destination count;
* rising/falling edge inputs;
* tri-state destinations;
* signal strength;
* multiple recursion modes.

A particular runtime output may know that it has:

```text
3 HIGH destinations
2 LOW destinations
no Hi-Z destinations
no strength-sensitive destinations
recursion disabled
```

The generated class can therefore contain only the required direct propagation operations.

Generic support for:

* unused destination types;
* recursion processing;
* tri-state handling;
* signal-strength argument propagation;

can be removed or specialised.

This is one of the main reasons optimisation occurs only after the complete circuit topology is known.

---

# Source class selection

`ClassOptimiser` can be created as:

```java
new ClassOptimiser<>(this)
```

or:

```java
new ClassOptimiser<>(this,SomeBaseClass .class)
```

The second form explicitly selects which generic source class should be transformed.

This is useful when runtime objects may already be subclasses but the desired optimisation template is a particular shared generic implementation.

The generated class still represents the concrete runtime optimisation variant derived from that selected source template.

---

# Optimiser correctness rules

Runtime optimisation is allowed to change implementation structure aggressively, but it must not change observable simulator semantics.

An optimiser implementation must preserve:

* signal values;
* Hi-Z behaviour;
* strong/weak signal semantics where applicable;
* edge-trigger semantics;
* ordering/priority requirements;
* recursion behaviour required by the configured mode;
* component-owned state dependencies;
* the no-unchanged-event Signal API invariant.

Optimisation may remove work only when the removed work is proven unnecessary for the runtime variant.

---

# State-removal rule

Particular care is required for:

```java
state =...
```

inside optimised inputs.

Removing the assignment is valid when the input is only an intermediate event-processing object and its local state is never read by hot-path component logic.

It is invalid when any component logic depends directly on:

```java
input.state
```

This dependency may exist in another pin's event handler.

Therefore the decision cannot be made by examining only the optimised method itself.

The whole schema-part state dependency must be considered.

---

# Reference-replacement rule

An optimisation that replaces an object must leave the runtime graph and schema-part references pointing to the replacement.

For outputs this is handled through `SchemaPart.replaceOut()` / `optimiseOuts()` followed by `initOuts()`.

For custom inputs the optimiser normally calls:

```java
parent.replaceIn(oldInput, newInput);
```

and the schema part must additionally update any private maps, arrays or fields that retain the old object.

Keeping a stale reference to a pre-optimisation object can silently bypass the optimised graph or produce incorrect state access.

---

# Generic implementation rule

Generic source must remain readable and functionally correct Java.

The preferred pattern is:

```text
correct generic implementation
        +
optimiser directives
        =
specialised implementation
```

rather than maintaining separate generic and optimised algorithms.

This keeps both implementations structurally tied to the same source logic and reduces semantic drift.

Optimiser directives should remove or substitute existing generic behaviour, not create an unrelated second implementation.

---

# Hot-path design

The optimiser exists specifically because some abstractions that are acceptable in normal application code are expensive in the simulation event path.

Optimised code should prefer, where safe:

* direct object references;
* direct field access;
* constants instead of runtime field reads;
* unrolled fixed-size propagation;
* early topology specialisation;
* removal of impossible branches.

Avoid adding abstraction layers merely for stylistic consistency when they introduce additional work into every signal event.

The generated code ultimately exists to give the JVM the simplest useful representation of the concrete circuit.

---

# Optimisation and JIT

`ClassOptimiser` does not replace JVM optimisation.

It changes the problem presented to the JVM.

Without source specialisation the JIT may see:

```text
runtime fields
branches
arrays
loops
multiple destination types
configuration-dependent behaviour
```

After source specialisation it can instead see:

```text
literal constants
direct final references
straight-line propagation
fewer branches
smaller methods
```

The optimisation pipeline is therefore:

```text
generic component
      |
      | KiCad topology/configuration
      v
NetFilter simplification
      |
      v
runtime graph construction
      |
      v
graph shortcutting
      |
      v
ClassOptimiser specialisation
      |
      v
runtime Java compilation
      |
      v
JVM JIT
      |
      v
machine code
```

Each stage removes information or structure that no longer needs to remain dynamic.

---

# Caching

A generated class is identified by its source class and optimisation variant.

The variant identity includes the active transformations such as:

* cuts;
* binds;
* unroll sizes;
* assertion mode.

Equivalent runtime objects can therefore reuse the same generated class.

This is important because a circuit may contain many instances of the same logical component with identical topology.

The specialised class is generated once and reused rather than recompiled for every individual component instance.

Generated source/class files may also be stored in the configured optimisation directory to reduce later startup work.

---

# Disabling the optimiser

The runtime optimiser can be disabled for debugging.

In this mode generic implementations remain in use.

This is useful for:

* comparing generic and optimised behaviour;
* diagnosing source-generation errors;
* isolating optimiser-specific bugs.

It is not the intended normal performance mode.

---

# Testing optimised schema parts

Schema-part behaviour must be tested in both:

```text
RAW
OPT
```

modes whenever custom runtime optimisation is implemented.

The same input/state sequence must produce the same externally visible result.

Spock data tables are particularly useful because the same logical cases can be executed against both variants.

For example:

```groovy
where:
optimized << [RAW, OPT]

combined:
a | b || expected
0 | 0 || 0
0 | 1 || 0
1 | 0 || 0
1 | 1 || 1
```

Tests should cover not only normal truth tables but also optimisation-dependent cases such as:

* initial state;
* edge inputs;
* tri-state transitions;
* state read by another pin;
* open-collector/open-emitter variants;
* different fan-out counts;
* recursive-event configurations where relevant.

Optimised behaviour being faster is useful only if it remains semantically identical.

---

# Optimiser design rules

When adding runtime optimisation:

* start from a correct generic implementation;
* specialise only from information known during runtime graph construction;
* use `cut` to remove impossible/unnecessary generic paths;
* use `bind` for values that are constant for the runtime variant;
* use `unroll` for small fixed-size hot propagation loops;
* preserve local input state whenever any hot-path component logic reads it;
* allow redundant intermediate state to disappear when it is not required;
* update all graph and schema-part references after object replacement;
* rebind final outputs through `initOuts()`;
* preserve the Signal API no-unchanged-event contract;
* do not depend on `getState()` source traversal in normal hot-path component logic;
* keep generic and optimised implementations behaviourally equivalent;
* test both RAW and OPT variants;
* treat `/*Optimiser ...*/` comments as executable source-transformation directives;
* optimise for a simpler final JVM hot path rather than for source-code abstraction alone.

---

# Summary

The runtime optimiser specialises a generic event-driven model into the concrete circuit that was actually loaded.

```text
generic implementation
        |
        | runtime topology known
        v
remove impossible behaviour
        |
        v
bind constants
        |
        v
unroll fixed topology
        |
        v
shortcut runtime objects
        |
        v
specialised Java class
        |
        v
JVM JIT
```

The central idea is:

> Do not repeatedly resolve at simulation time what is already known when the runtime circuit graph is built.

The optimiser converts topology and configuration knowledge into simpler objects and simpler Java code before those paths become hot.
