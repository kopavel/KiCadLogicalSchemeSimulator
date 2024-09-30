## SINGLE BIT BUFFER

Implements Single bit BUFFER/LATCH functionality.

**Input name:** D.  
**Output name:** Q.

**Optional parameter `latch`:** If provided, the schema part work as latch; otherwise — a buffer.

This schema part has two modes:

1. **Buffer Mode:** Additionally has a <span style="text-decoration: overline;">CS</span> input: Chip select.
    - At `Lo` state, directly transfers input to output.
    - At `Hi` state, output go to “High impedance” mode.

2. **Latch Mode:** Additionally has two input pins:
    - <span style="text-decoration: overline;">WR</span> input: Write. At negative front, data from input are stored internally in the latch.
    - <span style="text-decoration: overline;">OE</span> input: Output enable.
        - At `Lo` state, transfers latch stored states to output.
        - At `Hi` state, output go to “High impedance” mode.

For a latch provide the following parameters: `latch`.
