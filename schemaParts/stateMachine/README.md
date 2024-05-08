## State machine

Implements logical state machine functionality.

**Input names**:

- `INx` - inputs, where x a sequential number, starting from 0
- `S` - strobe. When `Hi`, inputs are stored to internal latch.
- `R` - reverse. When `Hi`, output are in reversed state.

**Output names**: OUT

**Mandatory parameters**:

- `size` - specifies the number of input pins.
- `outSize` - specifies the number of output pins.
- `states` - coma separated list on outpus state for each input combination.

For example, to create a XOR gate, you would provide the following parameters: `size=2;outSize=1;states=1,0,0,1`.

