## Binary/Decimal Counter

Implements a binary/decimal up/down counter with `PE`, `Carry in`, `Carry out` pins and 4 outputs.

**Input names**:

- `C` - clock input
- `PE` - Preset enabled
- `CI` - Carry in
- `BD` - Binary (Hi)/Decimal (Lo)
- `UD` - Up (Hi) /Down (Lo) direction
- `J` - Jam inputs
- `R` - Reset

**Output names**:

- `Qx` where x is a sequential number in range [0-3].
- `CO` - Carry out

**Optional parameters**

- `reverse`:** If provided, the `C` input is reversed (sensitive to negative front, positive otherwise).
- `carryReverse`:** If provided, the Carry input/output are reversed.
- `bdReverse`:** If provided, the `BD` input is reversed.

For example, to describe a counter sensitive to the negative front on the input you would provide the following parameter: `reverse`.
 
