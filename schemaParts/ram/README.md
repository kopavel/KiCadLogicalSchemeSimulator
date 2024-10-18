## RAM

Static RAM with a defined amount of address inputs and data outputs.

### Pins

#### Input names:

- `Ax`- address inputs
  x - sequential number in range [0…aSize].
- `CS`- Chip Select
- `WE`- Write Enable
- `OE`- Output Enable
- `Dx`- Data inputs
  x - a sequential number in range [0…size].

#### Output names:

- `Dx`- Data outputs
  x - a sequential number in range [0…size].

### Parameters

#### Mandatory parameters:

- `size`- data pin amount.
- `aSize`- address pin amount.
- `separateOut`- `Dx` bus inputs separated from Outputs and named Dinx, where x - sequential number in range [0…size].

#### Optional parameters:

- `reverse`- inputs CS, WE and OE reversed.

### Example

4Kb 8-bit RAM: `size=8;aSize=12`.
