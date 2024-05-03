## Multiplexer

Implements a multiplexer with a defined number of outputs and ways.

**Input names**:

- [Ax, Bx, ...] where x is a sequential number starting from 0.
- Nx where x is a sequential number starting from 0.

**Output names**: Qc, where c is a 'way' identifier, like A, B, ... (QA, QB, ...)

**Mandatory parameters**:

- `size`: Specifies the number of output pins in one 'way'.
- `nSize`: Specifies the number of 'way' identification pins. The 'way' size is calculated as 2^nSize.

For example, to describe a 4-way 4-pin multiplexer, you would provide the following parameters: `size=4;nSize=2`.
