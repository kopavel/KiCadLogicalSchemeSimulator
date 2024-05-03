## ROM

Implements a ROM with a defined number of address inputs and data outputs.

**Input names**:

- Ax, where x is a sequential number starting from 0
- CS (Chip Select)

**Output names**: Dx, where x is a sequential number starting from 0

**Mandatory parameters**:

- `size`: Specifies the number of output/data pins.
- `aSize`: Specifies the number of address pins.
- `file`: Specifies the path to the file (absolute or relative to the working folder) from which ROM content is populated.

**Optional parameter `reverse`**: If provided, the input CS is reversed, allowing for different configuration styles.

The file content is taken on a byte-by-byte basis if `size` is less than or equal to 8. For `size` greater than 8, the content is read in BigEndian order.
For example, to describe a 4Kb 8-bit ROM, populated from the file `bios.bin`, you would provide the following parameters: `size=8;aSize=12;file=bios.bin`.
