## SDRAM

Dynamic RAM with a defined amount of address inputs and data inputs/outputs.

### Pins

#### Input names:

- `Ax`- Address inputs.  
  x - sequential number in range [0…aSize-1]
- `Dx`- Data inputs.  
  x - sequential number in range [0…size-1]
- `RAS`- Row Address Strobe.  
  raising front sensitive.
- `CAS`- Column Address Strobe.  
  raising front sensitive.
- `WE`- Write Enable.

#### Output names:

- `Dx`- Data outputs.  
  x - sequential number in range [0…size-1]

### Parameters

#### Mandatory parameters:

- `size`- amount of data pins.
- `aSize`- amount of address pins.
- `separateOut`- `D` inputs separated from outputs and named `Dinx`, where x - sequential number in range [0…size-1].

#### Optional parameters:

- `reverse`- inputs `RAS`, `CAS` and `WE` reversed.

### Example

4Kb 8-bit SD-RAM: `size=8;aSize=6`.
