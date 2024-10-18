## RAM

Single bit static RAM with a defined amount of address inputs.

### Pins

#### Input names:

- `Ax`- address inputs
  x - sequential number in range [0…aSize].
- `CS`- Chip Select
- `WE`- Write Enable
- `OE`- Output Enable
- `D`- Data input

#### Output names:

- `D`- Data outputs

### Parameters

#### Mandatory parameters:

- `size`- address pin amount.
- `separateOut`- `D` bus inputs separated from Outputs and named Din.

#### Optional parameters:

- `reverse`- inputs CS, WE and OE reversed.

### Example

4Kb 1-bit RAM: `size=12`.
