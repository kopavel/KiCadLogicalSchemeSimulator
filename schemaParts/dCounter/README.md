## Binary/Decimal Counter

Implements a binary/decimal up/down counter with `PE`, `Carry in`, `Carry out` pins and 4 outputs.

### Pins

#### Input names:

- `C`- clock input.  
  sensible to raising edge.
- `PE`- Preset enabled.
- `CI`- Carry in.
- `BD`- Binary (Hi)/Decimal (Low).
- `UD`- Up (Hi) /Down (Low) direction.
- `J`- Jam inputs.
- `R`- Reset.
- `E`- Clock enable.

#### Output names:

- `Qx`- outputs.  
  x - sequential number in range [0-3].
- `CO`- Carry out

### Parameters

#### Mandatory parameters:

- none

#### Optional parameters:

- `reverse`- `C` sensitive to falling edge.
- `carryReverse`- `Carry id` and `Carry out` reversed.
- `bdReverse`- `BD` reversed.
- `eReverse`-  `E` reversed.

### Example

counter with `C` sensitive to falling edge: `reverse`.
 
