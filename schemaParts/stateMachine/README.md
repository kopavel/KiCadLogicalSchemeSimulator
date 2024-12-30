## State machine

Logical state machine.

### Pins

#### Input names:

- `INx`- Inputs.  
  x - sequential number in ranfe of [0…size-1].
- `C`- Clock.   
  `IN` input enable/latch
- `R`- reverse.   
  On active output in reversed state.
- `D`- disabled.  
  On active all outputs is `Low` (`Hi`, if R active).

#### Output names:

- `OUT`- Outputs.

### Parameters

#### Mandatory parameters:

- `size`- amount of input pins.
- `outSize`- amount of output pins.
- `states`- coma separated list of output states for each input combination.

#### Optional parameters:

- `latch` - latch mode.
  when set - `In` state latched at `C` raising edge.
  when not set - `In` state applied when `C` state is active.
- `cReverse` - `C` input reverse (falling edge/`low` state active)
### Example

XOR gate: `size=2;outSize=1;states=1,0,0,1`.

