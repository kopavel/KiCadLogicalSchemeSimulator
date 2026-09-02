# Schema Part Signal API

The simulator exposes a small signal API for implementing schema parts.

Schema-part implementations normally operate only with pins and buses. Internal graph management, connection objects and optimiser internals are not part of this
API.

The API is intentionally direct and performance-oriented.

---

# Signal event contract

A propagated signal event always represents a real state change.

This is a required part of the simulator API contract, not merely a performance recommendation.

When a model receives:

```java
setHi()

setLo()

setState(...)

setHiImpedance()
```

it may assume that the incoming signal represents a real transition.

The receiving model is therefore **not required to verify again that the incoming state differs from the previously propagated state**.

For example:

```java
setHi()
```

may be treated as a real LOW → HIGH transition.

Likewise:

```java
setLo()
```

may be treated as a real HIGH → LOW transition.

For a bus:

```java
setState(newState)
```

means that the propagated bus value has changed.

Propagating an unchanged signal is **forbidden**.

An unchanged call may otherwise be interpreted as a real event by downstream logic. This is important for correctness as well as performance: edge-sensitive inputs,
mergers, counters, retry/shortcut logic and other model elements may intentionally process every incoming call as a real event.

---

# Change detection and stop-fast

When component logic calculates an output whose new state may be equal to its current state, propagation must stop before an unchanged event is emitted.

For a pin:

```java
if(!out.state){
        out.

setHi();
}
```

```java
if(out.state){
        out.

setLo();
}
```

For a bus:

```java
if(out.state !=newState){
        out.

setState(newState);
}
```

For high impedance:

```java
if(!out.hiImpedance){
        out.

setHiImpedance();
}
```

Conceptually:

```text
component logic
      |
      | calculate possible new state
      v
is a change guaranteed?
      |
      +-- no ---> compare with current state
      |             |
      |             +-- same -----> STOP
      |             |
      |             +-- changed --+
      |                            |
      +-- yes ---------------------+
                                   |
                                   v
                              propagate
                                   |
                                   v
                        downstream assumes change
```

The comparison is **not required when the component semantics already guarantee that the output changes**.

For example, if a counter output is updated only as the direct consequence of a counter transition and its new value is known to differ from its previous value, it
may call:

```java
out.setState(newState);
```

without first performing:

```java
if(out.state !=newState)
```

The purpose of the contract is precisely to avoid redundant state checks throughout the propagation path.

Change detection should therefore be performed **once, at the earliest point where it is actually necessary**, and omitted when a change is guaranteed by the
component logic.

---

# Signal state storage

Pins and buses expose a public `state` field for hot-path access.

Schema-part logic may read:

```java
pin.state
```

or:

```java
bus.state
```

directly.

However, runtime optimisation does **not** require every object in the propagation chain to keep its own copy of the state.

The simulator may eliminate redundant state storage from intermediate objects and retain the authoritative state only at another object in the optimised signal path.

Conceptually:

```text
generic graph:

Out.state
   |
Intermediate.state
   |
Input.state


optimised graph:

Out.state
   |
Intermediate
   |
Input
```

Only one physical state copy may be necessary.

The generic API provides:

```java
getState()
```

which can obtain the effective state through the optimised `source` chain when the local object does not own the state.

This is intentionally **not the preferred mechanism for normal event-processing code**.

Following the source chain introduces extra method calls and indirection and is therefore reserved mainly for non-hot-path functionality such as:

* oscilloscope/debugging support;
* component monitoring;
* diagnostics;
* `toString()` and similar introspection.

Normal schema-part event logic should use direct field access when it requires a locally maintained signal state.

---

# State dependency in custom inputs

A custom input does not necessarily need to maintain its own local `state`.

If no component logic reads that input's state, the optimiser may remove the corresponding state assignment.

For example, an intermediate input implementation may process:

```java
setHi()
```

without keeping:

```java
state =true;
```

if that state is never read by the schema part.

However, if **any logic anywhere in the same schema part** directly reads that input's state, then its local state becomes part of the component contract and must be
preserved.

For example:

```java
class SomeChip {
    Pin enabled;
    Pin clock;
}
```

where the clock logic contains:

```java
if(enabled.state){
        ...
        }
```

In this case the implementation of `enabled` must keep its local state updated.

The optimiser must not remove:

```java
state =true;
```

or:

```java
state =false;
```

from that input, because another input or another part of the component logic relies on it.

The important rule is therefore not:

> an input must preserve its own state if it reads itself

but:

> an input must preserve local state whenever any hot-path component logic directly depends on that input's `state` field.

This applies equally to pins and buses.

See [Runtime Optimiser Design](../optimiser/DESIGN_Optimiser.md) for detailed source-optimiser rules.

---

# Wire API

Wire signals are represented by pin objects.

The schema-part-facing pin types are:

```text
InPin
TriStateInPin
RaisingEdgePin
FallingEdgePin

OutPin
TriStateOutPin
PullPin
PassivePin
```

## InPin

`InPin` is the basic digital input.

A custom input normally implements:

```java
setHi()

setLo()
```

Every invocation represents a real signal transition.

The implementation does not need to check again whether the transition occurred.

For example, `setHi()` may immediately execute HIGH-transition logic without first testing the previous state.

The input does not necessarily have to keep a local state copy.

If its state is not directly used by component logic, runtime optimisation may remove redundant state storage.

If its state is directly read anywhere in the component:

```java
if(someInput.state){
        ...
        }
```

then that input must maintain its local state.

---

## TriStateInPin

`TriStateInPin` is an input that also distinguishes high impedance.

In addition to:

```java
setHi()

setLo()
```

it handles:

```java
setHiImpedance()
```

Use this type only when component behaviour actually depends on distinguishing an electrically disconnected input from a driven LOW or HIGH signal.

A received high-impedance call represents a real transition.

---

## RaisingEdgePin

`RaisingEdgePin` is intended for inputs interested only in the LOW → HIGH transition.

Component-specific logic is implemented in:

```java
setHi()
```

Every invocation may be treated as a genuine rising edge.

The opposite transition is handled by the base implementation.

Use this type instead of a generic `InPin` followed by an explicit edge test.

---

## FallingEdgePin

`FallingEdgePin` is intended for inputs interested only in the HIGH → LOW transition.

Component-specific logic is implemented in:

```java
setLo()
```

Every invocation may be treated as a genuine falling edge.

The opposite transition is handled by the base implementation.

---

## OutPin

`OutPin` is the basic digital output.

Signal changes are emitted using:

```java
setHi()

setLo()
```

The caller must not emit an unchanged state.

If the new state may or may not differ, check it:

```java
if(!out.state){
        out.

setHi();
}
```

```java
if(out.state){
        out.

setLo();
}
```

If component logic guarantees the transition, the check should be omitted.

For example, code executing only on a known toggling transition may directly send the resulting output event.

Unlike many intermediate inputs, an output entering the model normally owns a reliable current state and can therefore be used for output-side change detection.

---

## TriStateOutPin

`TriStateOutPin` can produce:

```text
LOW
HIGH
HIGH IMPEDANCE
```

using:

```java
setHi()

setLo()

setHiImpedance()
```

The same event contract applies to all three states.

If the requested state might already be active, compare first.

If the component logic guarantees that the output is transitioning to a different state, no redundant comparison is necessary.

---

## PullPin

`PullPin` represents a weakly driven signal such as a pull-up or pull-down.

Use it when a schema part provides a weak default logical level rather than a normal actively driven output.

Strong/weak conflict resolution is handled by the simulator's merging logic.

---

## PassivePin

`PassivePin` is intended for passive components whose behaviour depends on the resolved state of the surrounding net.

Use it for components that cannot be represented simply as an independent input followed by an independent output.

---

# Bus API

Bus signals represent multi-bit values.

The schema-part-facing bus types are:

```text
InBus
OutBus
TriStateOutBus
```

Bus state is represented by:

```java
int state;
```

---

## InBus

`InBus` is a multi-bit input.

Custom logic normally implements:

```java
setState(int newState)
```

Every invocation represents a real propagated value change.

The implementation therefore does not need to compare `newState` with the previously propagated value merely to verify that an event occurred.

As with `InPin`, local `state` storage may be removed by optimisation when no hot-path component logic directly reads it.

If another part of the same schema part uses:

```java
someBus.state
```

then that bus must preserve its local state.

---

## OutBus

`OutBus` is a multi-bit output.

Set its value using:

```java
setState(int newState)
```

If the calculated value may equal the existing output:

```java
if(out.state !=newState){
        out.

setState(newState);
}
```

If the component semantics guarantee that `newState` differs, the comparison should be omitted.

Calling `setState()` with an unchanged value when no such guarantee exists violates the signal event contract.

Downstream bus logic may process every received `setState(...)` as a real value change.

---

## TriStateOutBus

`TriStateOutBus` is a multi-bit output that can additionally enter high impedance.

Use:

```java
setState(int newState)

setHiImpedance()
```

The same rules apply:

* compare when the transition is not known in advance;
* omit the comparison when the component logic guarantees a real transition;
* never intentionally propagate an unchanged state.

---

# Schema-part implementation rules

Schema-part implementations should follow these rules:

* Treat every received signal call as a real state change.
* Never intentionally propagate an unchanged output event.
* Perform change detection at the earliest place where it is actually necessary.
* Do not perform a redundant state comparison when component semantics already guarantee a transition.
* Do not re-check incoming events merely to confirm that they changed.
* Use direct `state` access in hot-path component logic.
* Do not use `getState()` in normal event processing when direct local state is required.
* Preserve local input state only when component logic actually depends on that input's `state`.
* Remember that another pin or bus in the same chip may depend on that state.
* Allow the optimiser to remove redundant state storage from intermediate signal objects when no direct dependency exists.
* Avoid unnecessary getters, setters and wrapper methods in signal-processing hot paths.
* Use edge-specific input types when only one transition matters.
* Use tri-state types only when high impedance is semantically relevant.
* Prefer early-return and stop-fast control flow.

Typical conditional wire output:

```java
if(condition){
        if(!out.state){
        out.

setHi();
    }
            }else if(out.state){
        out.

setLo();
}
```

Typical bus output when the value may remain unchanged:

```java
int newState = ...;
        if(out.state !=newState){
        out.

setState(newState);
}
```

Typical bus output when a change is guaranteed:

```java
out.setState(newState);
```

The central rule is:

```text
unchanged states must never enter the propagation graph,
but unnecessary checks must not be added when a change is already guaranteed.
```
