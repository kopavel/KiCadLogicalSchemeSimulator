## RAM

Implements static RAM with a defined number of address inputs and data outputs.

**Input names**:

- Ax, where x is a sequential number starting from 0.
- CS (Chip Select)
- WE (Write Enable)
- OE (Output Enable)

**Output names**: Dx, where x is a sequential number starting from 0.

**Mandatory parameters**:

- `size`: Specifies the number of output/data pins.
- `aSize`: Specifies the number of address pins.

**Optional parameter `reverse`**: If provided, the inputs CS, WE and OE are reversed, allowing for different configuration styles.

For example, to describe a 4Kb 8-bit RAM, you would provide the following parameters: `size=8;aSize=12`.
