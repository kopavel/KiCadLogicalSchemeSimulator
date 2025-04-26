## BUFFER/LATCH

### Pins

#### Input names:

- `Dx`- Data input bus.  
  x - Data input number in range [0…size-1].
- `CS`- Chip select.   
  Only in buffer mode.
  - `Hi`- transfers input bus state to output bus.
  - `Low`- output bus go to “High impedance” mode.
- `WR`- Write.  
  Only in latch mode.
  - On falling edge data from input bus stored internally in the latch.
- `OE`- Output enable.  
  Only in latch mode.
  - `Hi`- transfers latch state to output bus.
  - `Low`- output bus go to “High impedance” mode.

#### Output names:

- `Qx`- Data output bus.
  x — Data output number in range [0…size-1].

### Parameters

#### Mandatory parameters:

- `size`- Data bus width in range [2…32].

#### Optional parameters:

- `latch`- latch mode.
- `reverse`- reverse WR, OE and CS inputs.

### Example

8 bit width latch with `OE` active on `Low` and `WR` sensitive to falling edge: `size=8;latch;reverse`.
