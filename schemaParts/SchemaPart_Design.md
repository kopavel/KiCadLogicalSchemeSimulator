# Schema Part Design

A schema part is the simulator-side implementation of a logical component.

Each schema part:

* is created through `SchemaPartSpi`;
* receives its instance ID and component parameters;
* registers its input and output pins/buses;
* implements component-specific behaviour using the Signal API;
* may participate in runtime optimisation;
* may optionally expose interactive or monitoring UI.

The base implementation is:

```java
SchemaPart
```

A schema part implementation normally has the following structure:

```java
public class MyChip extends SchemaPart {
    public MyChip(String id, String params) {
        super(id, params);

        // register inputs and outputs
    }

    @Override
    public void initOuts() {
        // bind final output references
        // initialise state dependent on the constructed model
    }
}
```

---

# SchemaPart SPI

Schema parts are discovered through:

```java
SchemaPartSpi
```

The SPI contains two methods:

```java
SchemaPart getSchemaPart(String id, String params);

Class<? extends SchemaPart> getSchemaPartClass();
```

A typical implementation is:

```java
public class MyChipSpi implements SchemaPartSpi {
    @Override
    public SchemaPart getSchemaPart(String id, String params) {
        return new MyChip(id, params);
    }

    @Override
    public Class<? extends SchemaPart> getSchemaPartClass() {
        return MyChip.class;
    }
}
```

The schema-part module must publish the SPI implementation through the Java module descriptor:

```java
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import my.package.MyChipSpi;

open module KiCadLogicalSchemeSimulator.components.MyChip {
    requires KiCadLogicalSchemeSimulator.simulator;

    provides SchemaPartSpi with MyChipSpi;
}
```

The simulator can then discover the implementation without having a direct dependency on the schema-part module.

---

# Construction

Every schema part receives:

```java
String id
String params
```

through its constructor:

```java
public MyChip(String id, String params) {
    super(id, params);
}
```

`id` identifies the concrete component instance in the simulated schema.

`params` contains configuration specific to the schema-part implementation.

Construction is also the normal place to define the external signal interface of the component.

For example:

```java
public MyChip(String id, String params) {
    super(id, params);

    addInPin(...);
    addInPin(...);

    addOutPin("OUT");
}
```

Pins and buses registered during construction become the interface used when the KiCad net is connected to the runtime schema-part graph.

---

# Parameters

The base `SchemaPart` parses the parameter string into:

```java
params
```

Parameters are separated by `;`.

A parameter may either contain a value:

```text
size=8
```

or act as a flag:

```text
reverse
```

For example:

```text
size=8;reverse;openCollector
```

can be used as:

```java
int size = Integer.parseInt(params.get("size"));

if(params.

containsKey("openCollector")){
        ...
        }
```

Flag parameters do not require a value.

Schema-part implementations are responsible for validating parameters required by their own logic.

For example:

```java
if(!params.containsKey("size")){
        throw new

RuntimeException(
            "Component "+id +" has no parameter \"size\"");
}
```

Parameters may originate from symbol mappings or from configuration for a specific schema-part instance.

Typical use is to keep properties inherent to a symbol in the symbol mapping:

```text
size=8
```

while instance-specific configuration can be supplied separately.

The final schema part receives the resolved parameter string and normally does not need to know where an individual parameter originated.

---

# Reverse logic

`SchemaPart` provides the common:

```java
reverse nReverse
```

flags.

The parameter:

```text
reverse
```

sets:

```java
reverse =true;
nReverse =false;
```

Otherwise:

```java
reverse =false;
nReverse =true;
```

These fields are a convenience for components that have logically inverted variants, such as AND/NAND or OR/NOR implementations.

For example:

```java
out.state =condition ?nReverse :reverse;
```

allows the same implementation to represent both variants.

---

# Registering inputs and outputs

All externally connected pins and buses must be registered through `SchemaPart`.

The registration API defines whether an item is an input, output, passive or bidirectional connection.

Signal behaviour itself is described in the Signal API design document.

## Input pin

For a simple input whose state only needs to be stored:

```java
InPin in = addInPin("IN");
```

For component-specific behaviour, create a custom input:

```java
InPin in = addInPin(new InPin("IN", this) {
    @Override
    public void setHi() {
        ...
    }

    @Override
    public void setLo() {
        ...
    }
});
```

Normally specialised input classes are preferable for non-trivial components.

For example:

```java
clock = addInPin(new RaisingEdgePin("CLK", this) {
    @Override
    public void setHi() {
        ...
    }
});
```

---

# Output pin

A normal output is registered using:

```java
addOutPin("OUT");
```

An initial logical state may be specified:

```java
addOutPin("OUT",true);
```

Tri-state output:

```java
addTriStateOutPin("OUT");
```

A tri-state output without an explicit initial driven state starts in high impedance.

An initial driven state may instead be specified:

```java
addTriStateOutPin("OUT",false);
```

Pull output:

```java
addPullPin("OUT",true);
```

Passive pins are registered using:

```java
addPassivePin(passivePin);
```

---

# Input bus

A normal multi-bit input can be created using:

```java
InBus bus = addInBus("D", 8);
```

For custom input behaviour:

```java
InBus bus = addInBus(new InBus("D", this, 8) {
    @Override
    public void setState(int newState) {
        ...
    }
});
```

---

# Output bus

Normal output bus:

```java
addOutBus("D",8);
```

With an initial value:

```java
addOutBus("D",8,initialState);
```

Tri-state output bus:

```java
addTriStateOutBus("D",8);
```

or with an initially driven state:

```java
addTriStateOutBus("D",8,initialState);
```

---

# Bus aliases

A bus may expose names for individual bits.

For example:

```java
addOutBus(
        "D",
                4,
                "D0",
                "D1",
                "D2",
                "D3");
```

The bus remains one runtime signal object while its aliases may be referenced individually by schema mapping.

Aliases are registered together with the bus and do not need to be separately added as pins.

When explicit aliases are not supplied, default aliases are derived from the bus ID and bit number.

The alias count must correspond to the bus width.

---

# Bidirectional connections

A schema connection may act as both an input and an output.

For pins, register the input first and then the output with the same ID:

```java
addInPin(...);

addOutPin("D");
```

The connection is then classified as bidirectional.

Input and output remain separate runtime signal objects because they represent different propagation directions.

The same concept applies to buses.

Component logic should therefore keep separate input and output references even when they correspond to the same physical KiCad pin or bus.

---

# Output references and `initOuts()`

Output objects are not guaranteed to retain their original identity during model construction.

Runtime graph optimisation may replace an output with another implementation, shortcut it to another object or generate a specialised output class.

For this reason, schema-part logic must not assume that an output object obtained during construction remains the final runtime output.

Final output references must be obtained in:

```java
initOuts()
```

using:

```java
getOutPin(...)

getOutBus(...)
```

For example:

```java
private Pin out;

public MyChip(String id, String params) {
    super(id, params);
    addOutPin("OUT", false);
}

@Override
public void initOuts() {
    out = getOutPin("OUT");
}
```

This means output references that are used by component logic should normally **not be `final`**.

`initOuts()` is called after output optimisation/finalisation and is the place to:

* obtain the final runtime output objects;
* pass final output references to custom input objects;
* initialise state that depends on the complete schema-part signal structure.

For example:

```java
@Override
public void initOuts() {
    out = getOutPin("OUT");

    input.out = out;

    out.state = calculateInitialState();
}
```

This separation is intentional:

```text
constructor
    |
    | declare/register component interface
    v
model interconnection
    |
    v
runtime optimisation
    |
    v
initOuts()
    |
    | bind final output references
    v
simulation
```

---

# Optimised custom inputs

Input objects may also be replaced by their optimised implementations.

The base `SchemaPart` updates its internal input registration when this happens.

However, a component may also keep its own references to custom inputs:

```java
private final Map<String, MyInput> inputs = new HashMap<>();
```

If such an input can be replaced by the optimiser, the component must update its own reference as well.

Override:

```java
replaceIn(...)
```

and call the base implementation first:

```java
@Override
public <T> void replaceIn(ModelItem<T> oldIn, ModelItem<T> newIn) {
    super.replaceIn(oldIn, newIn);

    inputs.put(oldIn.getId(), (MyInput) newIn);
}
```

This requirement applies only to component-owned references that may otherwise continue pointing to the pre-optimisation object.

Detailed optimiser behaviour and source-optimisation rules are documented separately.

---

# Initial state

Initial output state should be established without generating artificial runtime events.

Where possible, use the registration overload that accepts an initial state:

```java
addOutPin("OUT",false);
```

or:

```java
addOutBus("OUT",8,initialState);
```

When the initial value depends on component inputs or configuration, initialise the final output object during `initOuts()`:

```java
@Override
public void initOuts() {
    out = getOutPin("OUT");
    out.state = calculateInitialState();
}
```

Initialisation is different from event propagation.

During initial model setup it is valid to assign the initial state directly. During normal simulation, output changes must follow the Signal API event contract.

---

# Reset

A schema part may optionally implement:

```java
reset()
```

The default implementation does nothing.

Override it when the component contains internal state that must be restored by simulator reset.

For example:

```java
@Override
public void reset() {
    counter = 0;
    ...
}
```

Reset logic should restore component-internal state consistently with its signal state.

---

# Component-owned state

A schema part may keep internal fields required to implement the behaviour of the physical/logical component:

```java
int counter;
int address;
boolean enabled;
```

Do not duplicate pin or bus state into separate fields unless the component semantics require a distinct internal state.

When the current state of an input pin or bus is required by another part of the same component, the corresponding signal object's direct `state` field should
normally be used as described by the Signal API contract.

---

# Monitoring and interactive UI

`SchemaPart` also provides hooks used for monitoring and UI integration.

Those APIs are not part of the core schema-part signal contract and are documented separately in the UI design document.

A schema part does not need to provide UI functionality unless the component is interactive or exposes additional monitoring information.

---

# Testing schema parts

Schema-part modules have Spock available for component tests.

Spock is particularly useful for logical components because input combinations and expected outputs can be expressed directly as data tables.

The project provides `ChipSpec` support for testing schema parts.

A typical definition is:

```groovy
class AndTest extends ChipSpec {

    @Override
    protected ChipDefinition chip() {
        new ChipDefinition(
                new AndGateSpi(),
                "size=2",
                ["IN0", "IN1"],
                ["OUT"]
        )
    }
}
```

Logical behaviour can then be expressed as a state table:

```groovy
@Unroll("#optimized | #a AND #b -> #expected")
def "AndGate"() {
    given:
    useChip(optimized)

    when:
    setInputs(a, b)

    then:
    checkOutputs(expected)

    where:
    optimized << [RAW, OPT]

    combined:
    a | b || expected
    0 | 0 || 0
    0 | 1 || 0
    1 | 0 || 0
    1 | 1 || 1
}
```

Where runtime optimisation exists, component behaviour should be tested in both modes:

```text
RAW
OPT
```

The same logical state table should produce the same externally visible result for the generic and optimised component implementations.

This is particularly important because optimiser directives may remove fields, state assignments, branches or intermediate signal objects while preserving the
schema-part contract.

---

# Schema-part implementation rules

When implementing a schema part:

* extend `SchemaPart`;
* provide a `SchemaPartSpi` implementation;
* publish the SPI using `provides SchemaPartSpi with ...`;
* register all externally connected pins and buses during construction;
* validate required component parameters during construction;
* treat `params` as the resolved component configuration;
* use the Signal API for runtime signal behaviour;
* obtain final output references in `initOuts()`;
* do not assume constructor-created output objects survive runtime optimisation;
* update component-owned custom-input references through `replaceIn()` when those inputs are optimised;
* use direct state assignment for model initialisation, not for normal event propagation;
* implement `reset()` only when the component has resettable internal state;
* test logical behaviour with Spock state tables;
* test both `RAW` and `OPT` modes for optimised components.
