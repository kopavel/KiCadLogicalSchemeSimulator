## SDRAM

Implements dynamic RAM with a defined number of address inputs and data outputs.

**Input names**:

- Ax, where x is a sequential number starting from 0
- RAS (Row Address Strobe)
- CAS (Column Address Strobe)
- WE (Write Enable)

**Output names**: Dx, where x is a sequential number starting from 0

**Mandatory parameters**:

- `size`: Specifies the number of output/data pins.
- `aSize`: Specifies the number of address pins.

**Optional parameter `reverse`**: If provided, the inputs RAS, CAS, and WE are reversed, allowing for different configuration styles.

For example, to describe a 4Kb 8-bit SDRAM, you would provide the following parameters: `size=8;aSize=12`.
