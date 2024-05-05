## BUFFER

Implements BUFFER/LATCH functionality.

**Input names**: Dx, where x a sequential number, starting from 0.  
**Output names**: Qx, where x a sequential number, starting from 0.

**Mandatory parameter `size`:** Specifies the number of input/output pins.  
**Optional parameter `latch`:** If provided, the schema part is a latch; otherwise, it's a buffer.

This schema part has two modes, both with `<size>` number of inputs and outputs:

1. **Buffer Mode:** Additionally has a <span style="text-decoration: overline;">CS</span> input: Chip select.
    - At `Lo` state, directly transfers inputs to outputs.
    - At `Hi` state, outputs are in "High impedance" mode.

2. **Latch Mode:** Additionally has two input pins:
    - <span style="text-decoration: overline;">WR</span> input: Write. At negative front, data from inputs are stored internally in the latch.
    - <span style="text-decoration: overline;">OE</span> input: Output enable.
        - At `Lo` state, transfers latch stored states to outputs.
        - At `Hi` state, outputs are in "High impedance" mode.

For example, to create a 8-input latch, you would provide the following parameters: `size=8;latch`.
