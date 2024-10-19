## State machine

Logical state machine.

### Pins

#### Input names:

- `INx`- Inputs.  
  x - sequential number in ranfe of [0…size-1].
- `C`- Clock.   
  only in `latch` mode.   
  on raising edge, inputs stored to internal latch.
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
- `latch`- latch mode. `In` state only applied at `C` raising edge.
- `states`- coma separated list of output states for each input combination.

### Example

XOR gate: `size=2;outSize=1;states=1,0,0,1`.

