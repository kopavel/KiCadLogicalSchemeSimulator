## Decoder

Implements binary decoder.

**Input names**:

- Ax, where x is a sequential number, starting from 0;
- `CS` - chip select

**Output names**:

- Qx, where x is a sequential number, starting from 0

**Mandatory parameter `size`:** Specifies the input pins amount.

**Optional parameters**:

- `reverse`: If provided, the `CS` input are reversed.
- `outReverse`: If provided, the outputs are reversed.
