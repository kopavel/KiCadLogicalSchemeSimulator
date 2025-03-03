## Ring Counter

Implements a Ring counter with a defined amount of outputs.

### Pins

#### Input names:

- `C`- Clock  
  sensitive to raising edge
- `R`- Reset  
  active at `Hi`
- `CI`- Carry-in / Clock inhibit
  active at `Hi`

#### Output names:

- `CO`- Carry-out
- `Qx`- Output  
  x - a sequential number in ange [0…size-1]

### Parameters

#### Mandatory parameters:

- `size`- amount of output pins.  
  must be a multiple of 2.

#### Optional parameters:

- `reverse` - `C` input sensitive to the falling edge and `R` active at `Low`.

### Example

10-output (decade) Ring counter with `C` sensitive to falling edge:  
`size=10;reverse`.
