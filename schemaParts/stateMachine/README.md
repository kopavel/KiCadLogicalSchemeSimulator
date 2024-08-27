## State machine

Implements logical state machine functionality.

**Input names**:

- `INx` - inputs, where x a sequential number, starting from 0
- `S` - strobe. When `Hi`, inputs stored to internal latch.
- `R` - reverse. When `Hi`, output in reversed state.
- `D` - disabled. When `Hi`, all outputs state is Lo (Hi, if R active).

**Output names**: OUT

**Mandatory parameters**:

- `size` - specifies input pins amount.
- `outSize` - specifies output pins amount.
- `latch` - if presented latch IN only with S front, apply IN change immediately otherwise.
- `states` - coma separated list of output states for each input combination.

Following parameters, as example, create XOR gate, : `size=2;outSize=1;states=1,0,0,1`.

