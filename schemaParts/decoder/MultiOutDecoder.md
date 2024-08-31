## MultiOutDecoder

Implements a binary decoder with multiple output pin groups.

**Input names**:

- `Ax`, where `x` is a sequential number, starting from 0.
- `CSxn` - Chip Select, where `x` is a part identifier in the range [a…z] and `n` is a sequential number starting from 0.

**Output names**:

- `Qxn`, where `x` is a part identifier in the range [a…z], and `n` is a sequential number starting from 0.

**Mandatory parameters**:

- `size`: Specifies the number of `A` pins.
- `cs`: Specifies the parts/CS pins count and "reverse" mode for each.
    - Provided as a comma-separated list for parts like `<part>,<part>`,
    - where `<part>` is a colon-separated list like `N:R:R`.
    - If the input is `R`, it means reverse mode.

**Optional parameter `reverse`**: If provided, the outputs are reversed.

**Example**: Describing a decoder:

- With two `A` pins: `size=2`.
- With reversed output pins: `reverse`.
- Group A has 2 CS pins, one of which is reversed: `N:R`.
- Group B has 2 CS pins, both of which are reversed: `R:R`.

The full parameter list is: `size=2;reverse;cs=N:R,R:R`.
