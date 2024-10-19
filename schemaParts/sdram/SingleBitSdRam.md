## SingleBitSdRam

Dynamic RAM with a defined amount of address inputs and single data input/output.

### Pins

#### Input names:

- `Ax`- Address inputs.  
  x - sequential number in range [0…size-1]
- `Din`- Data input.
- `RAS`- Row Address Strobe.  
  raising front sensitive.
- `CAS`- Column Address Strobe.  
  raising front sensitive.
- `WE`- Write Enable.

#### Output names:

- `Dout`- Data output.

### Parameters

#### Mandatory parameters:

- `size`- amount of address pins.

#### Optional parameters:

- `reverse`- inputs `RAS`, `CAS` and `WE` reversed.

### Example

4Kb single bit SD-RAM: `size=6`.
