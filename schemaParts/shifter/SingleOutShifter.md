## SingleOutShifter

Implements a bit shifter with single bit output.

**Input names**:

- `Dx`- parallel load input, where x a sequential number, starting from 0.
- `CP` - clock input. Shift bits to High side on Rising edge, if DS is `Hi`, set first bit to `Hi`.
- `CN` - clock input. Shift bits to Lo side on Rising edge, if DS is `Hi`, set the highest bit to `Hi`.
- `CO` - clock inhibit. If active — clock inputs ignored.
- `PL` - parallel load. If active — on clock input rising edge load `Dx` inputs.
- `DS` - serial load input.
- `R` - shifter reset. If active — clock inputs ignored.

**Output names**:

- `Q` - shifter output.

**Mandatory parameters:**

- `size`: specifies size of shifter in bits.
- `outPin`: specifies position/number for Q output, starting from 0.

**Optional parameters:**

- `reverse`: if provided, the clock inputs reversed.
- `plReverse`: if provided, the `PL` input reversed.
- `clearReverse`: if provided, the `R` input reversed.
- `inhibitReverse`: if provided, the `CI` input reversed.

**Example**  
8-bit shifter with <span style="text-decoration: overline;">CP</span> and Q from the highest bit: `size=8;outPin=7;reverse`.

