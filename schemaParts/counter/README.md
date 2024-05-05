## Binary Counter

Implements a binary counter with a defined number of outputs.

**Input names**:

- `C` - counter input
- `R` - reset input

**Output names**: Qx, where x is a sequential number, starting from 0.

**Mandatory parameter `size`:** Specifies the number of output pins.  
**Optional parameter `reverse`:** If provided, the `C` input is reversed (sensitive to negative front, positive otherwise).

For example, to describe a 4-output counter sensitive to the negative front on the input you would provide the following parameters: `size=4;reverse`.
 
