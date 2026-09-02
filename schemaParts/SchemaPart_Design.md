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

# UI integration

A schema part may optionally expose a visual component.

UI support is not required for normal schema parts and is intentionally separated from the simulation signal API.

There are two UI mechanisms:

```text
InteractiveSchemaPart
    -> persistent component shown on the simulator desktop

SchemaPart monitoring
    -> optional diagnostic state/panel shown by SchemaPartMonitor
```

Neither mechanism should become part of the normal signal-propagation path.

---

# Interactive schema parts

A schema part that needs a persistent visual representation implements:

```java
InteractiveSchemaPart
```

The interface contains a single method:

```java
AbstractUiComponent getComponent();
```

For example:

```java
public class MyPart extends SchemaPart implements InteractiveSchemaPart {
    private final MyUiComponent uiComponent;

    public MyPart(String id, String params) {
        super(id, params);

        ...
        uiComponent = new MyUiComponent(id, this);
    }

    @Override
    public AbstractUiComponent getComponent() {
        return uiComponent;
    }
}
```

The component may also be created lazily when first requested.

The simulator discovers interactive schema parts through the interface and places their UI components on the simulator desktop.

---

# AbstractUiComponent

Interactive UI components extend:

```java
AbstractUiComponent
```

A component provides:

```java
protected AbstractUiComponent(String title, int size)
```

where:

* `title` identifies the schema-part instance;
* `size` defines the base size of the component's visual area.

Custom drawing is implemented through:

```java
protected void draw(Graphics2D g2d)
```

For example:

```java
public class MyUiComponent extends AbstractUiComponent {
    private final MyPart parent;

    public MyUiComponent(String title, MyPart parent) {
        super(title, 30);
        this.parent = parent;
    }

    @Override
    protected void draw(Graphics2D g2d) {
        ...
    }
}
```

`AbstractUiComponent` provides the common component title, sizing and simulator layout behaviour.

Interactive components can be moved on the simulator desktop. Their position and scale are managed by the simulator UI/layout infrastructure and should not normally
be managed by the schema-part implementation.

---

# Model to UI

UI rendering may inspect schema-part state.

A simple component can expose state through a provider:

```java
uiComponent =new

MyUiComponent(id,
        this::isActive);
```

and read it while drawing:

```java
g2d.setColor(provider.isActive() ?onColor :offColor);
```

This keeps presentation logic outside the schema-part event implementation.

For rapidly changing signals, UI updates should not be inserted into every simulation event merely to keep the screen current.

The simulation event rate can be much higher than the useful UI refresh rate.

Prefer:

```text
simulation
    |
    | update model state
    v
model
    |
    | sampled/read periodically
    v
UI redraw
```

instead of:

```text
simulation event
    |
    v
UI update
    |
    v
simulation event
    |
    v
UI update
    ...
```

`AbstractUiComponent.redrawPeriod` provides the common redraw period currently used by simple periodically refreshed components.

For more complex UI, a schema part may maintain presentation state and request repaint only when useful.

The important design rule is:

> UI refresh must not add unnecessary work to the main signal propagation path.

---

# UI to model

Interactive components may also initiate component behaviour.

For example, a UI switch may call:

```java
parent.toggle(newState);
```

or a keyboard component may call:

```java
parent.keyEvent(key, pressed);
```

The schema part remains responsible for translating the UI action into simulator signal changes.

Conceptually:

```text
user interaction
      |
      v
UI component
      |
      | component-level operation
      v
SchemaPart
      |
      | Signal API
      v
simulation graph
```

The UI component should not manipulate the simulator graph directly.

It should invoke an operation on its owning schema part and let the schema part apply the normal signal-event contract.

Therefore signal changes initiated from UI must obey exactly the same rules as all other schema-part output changes:

* do not propagate unchanged states;
* use normal Pin/Bus signal methods;
* preserve stop-fast behaviour;
* treat propagated events as real state changes.

Thread hand-off, if required by a particular interactive component, is component-specific. The UI API does not introduce a separate asynchronous event model for
schema parts.

---

# UI state and simulation state

The schema part is the owner of component behaviour.

The UI component is a representation and/or user interface for that behaviour.

Do not make UI-only state the authoritative state of a simulated component when that state affects circuit behaviour.

Prefer:

```text
SchemaPart state
      |
      +----> simulation behaviour
      |
      +----> UI representation
```

rather than:

```text
UI state
   |
   +----> simulation behaviour
```

UI-local state is appropriate for presentation-only details such as:

* cached drawing geometry;
* currently displayed color;
* mouse position;
* temporary labels;
* visual scale/layout information.

Circuit-relevant state belongs to the schema part.

---

# Schema-part monitoring

Every schema part can optionally expose additional diagnostic information through:

```java
String extraState()
```

The default implementation returns:

```java
null
```

meaning that no additional state is available.

A component may override it:

```java

@Override
public String extraState() {
    return "Counter:" + counter;
}
```

or return multiple lines:

```java

@Override
public String extraState() {
    return "Address:" + address + "\nData:" + data;
}
```

`extraState()` is consumed by the schema-part monitoring UI.

It is intended for diagnostics and observation, not for simulation behaviour.

Because monitoring is outside the normal event model, `extraState()` may use operations that would be inappropriate in a signal hot path, including formatting and
state inspection through slower APIs where necessary.

It should not modify simulation state.

---

# Extra monitoring panel

A schema part may additionally expose a custom monitoring panel through:

```java
Supplier<JPanel> extraPanel()
```

The default implementation returns:

```java
null
```

A component can provide one when richer diagnostic information is useful:

```java

@Override
public Supplier<JPanel> extraPanel() {
    return () -> new MemoryDumpPanel(memory);
}
```

A `Supplier` is used so the additional UI does not have to be created unless the user actually opens it.

This mechanism is appropriate for diagnostic views such as:

* memory dumps;
* internal register/state views;
* component-specific debugging panels.

It is separate from `InteractiveSchemaPart`.

A component may therefore have:

```text
no UI
```

or:

```text
InteractiveSchemaPart
```

or:

```text
extraState / extraPanel
```

or both.

---

# UI design rules

When adding UI to a schema part:

* implement `InteractiveSchemaPart` only when the component needs a persistent simulator-desktop representation;
* return an `AbstractUiComponent` from `getComponent()`;
* implement custom rendering in `draw(Graphics2D)`;
* keep circuit behaviour in the schema part, not in the UI component;
* let UI actions call component-level methods on the schema part;
* apply the normal Signal API contract to events initiated by UI;
* avoid adding redraw work to every hot-path simulation event;
* prefer UI sampling/polling or aggregated refresh for rapidly changing state;
* use `extraState()` for lightweight diagnostic text;
* use `extraPanel()` for richer optional diagnostic UI;
* keep monitoring code observational and outside normal simulation behaviour.

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
