## RAM

Implements static RAM with a defined number of address inputs and data outputs.

**Input names**:

- A[x], where x is a sequential number, starting from 0.
- CS (Chip Select)
- WE (Write Enable)
- OE (Output Enable)

**Output names**: D[x], where x is a sequential number, starting from 0.

**Mandatory parameters**:

- `size`: Specifies output/data pin amount.
- `aSize`: Specifies address pin amount.
- `separateOut`: If specified — D bus inputs are separated from Outputs and named Din[x], where x is a sequential number, starting from 0.

**Optional parameter `reverse`**: If provided, the inputs CS, WE and OE are reversed, allowing for different configuration styles.

For example, to describe a 4Kb 8-bit RAM, you would provide the following parameters: `size=8;aSize=12`.
