## Multiplexer

Implements a multiplexer with a defined output amount and ways.

**Input names**:

- `[Ax, Bx, …]` where x is a sequential number, starting from 0.
- `Nx` where x is a sequential number, starting from 0.
- `OE` output enabled, if Lo — output is in `Hi Impedance` state.

**Output names**: Qc, where c is a 'way' identifier, like A, B, ... (QA, QB, ...)

**Mandatory parameters**:

- `size`: Specifies the amount of output pins in one 'way'.
- `nSize`: Specifies the amount of 'way' identification pins. The 'way' size calculated as 2^nSize.

**Optional parameter `reverse`**: it true — OE input is reversed.

For describe 4-way 4-pin multiplexer, provide the following parameters: `size=4;nSize=2`.
