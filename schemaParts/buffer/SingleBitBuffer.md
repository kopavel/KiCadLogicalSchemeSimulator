## SINGLE BIT BUFFER

Implements Single bit BUFFER/LATCH functionality.

### Pins

#### Input names;

- `D` Data input pin
- `CS` Chip select.   
  Only in buffer mode.
  - `Hi` transfers input pin state to output pin.
  - `Lo` output pin go to “High impedance” mode.
- `WR` Write.  
  Only in latch mode
  - On falling edge data from input stored internally in the latch.
- `OE` Output enable.  
  Only in latch mode
  - `Hi` transfers latch state to output pin.
  - `Lo` output pin go to “High impedance” mode.

#### Output names:

- `Q` Data output pin

### Parameters

#### Mandatory parameters:

- none

#### Optional parameters:

- `latch` - latch mode
- `reverse` - reverse WR, OE and CS inputs.

### Example:

Latch with `OE` active on `Lo` and `WR` sensitive to falling edge: `latch;reverse`.
