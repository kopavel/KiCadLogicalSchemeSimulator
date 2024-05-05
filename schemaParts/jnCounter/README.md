## Johnson Counter

Implements a Johnson counter with a defined number of outputs.

**Input names**: C, R, CI  
**Output names**: CO, Qx, where x is a sequential number, starting from 0.

**Mandatory parameter `size`**: Specifies the number of output pins. The value must be a multiple of 2.    
**Optional parameter `reverse`**: If provided, the C input is sensitive to the negative front (it is sensitive to the positive front otherwise).

**Pin Descriptions**:

- C: Clock input
- CI: Clock inhibit
- R: Reset input
- Qx: Outputs
- CO: Carry out output

For example, to describe a 10-output (decade) Johnson counter that is sensitive to the negative front on the clock input, you would provide the following
parameters: `size=10;reverse`.
