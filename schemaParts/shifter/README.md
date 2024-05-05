## Shifter

Implements a bit shifter.

**Input names**:

- `Dx`, where x a sequential number, starting from 0
- `CP` - shift bits to High side on Rising edge, if DS is `Hi`, set first bit to `Hi`,
- `CN` - shift bits to Lo side on Rising edge, if DS is `Hi`, set the highest bit to `Hi`,
- `PL` - parallel load, if `HI` - clock inputs are ignored - Input instantly loaded to internal register and outputted to outs.
- `DS` - serial input

**Output names**:

- Qx, where x a sequential number, starting from 0

**Mandatory parameter `size`:** specifies the number of D/Q input/output pins.  
**Optional parameter `reverse`:** if provided, the CN and CP inputs are reversed
**Optional parameter `plReverse`:** if provided, the PL input is reversed

For example, to create 8-bit shifter with <span style="text-decoration: overline;">CP</span> you would provide the following parameters: `size=8;reverse`.

