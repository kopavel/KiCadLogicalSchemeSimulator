## SDRAM

Dynamic RAM with a defined amount of address inputs and single data output.

**Input names**:

- `Ax`, where x is a sequential number, starting from 0
- `RAS` (Row Address Strobe)
- `CAS` (Column Address Strobe)
- `WE` (Write Enable)
- `Din` Data input

**Output name**: `Dout` Data output

**Mandatory parameter**: `size`: Specifies the number of address pins.

**Optional parameter `reverse`**: If provided, the inputs RAS, CAS, and WE are reversed, allowing for different configuration styles.

For describe a 4096x1 bit SDRAM, you would provide the following parameters: `size=6`.
