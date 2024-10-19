## SingleOutShifter

Bit shifter with single bit output.

### Pins

#### Input names:

- `Dx`- parallel load input  
  x - sequential number in range of [0…size-1].
- `CP` - Positive shift clock input.  
  Shift bits to High side on Rising edge. If `DS` Hi, set first bit to Hi.
- `CN` - Negative shift clock input.   
  Shift bits to Lo side on Rising edge. If `DS` Hi, set the highest bit to Hi.
- `CO` - clock inhibit.  
  If active (Hi)— clock inputs ignored.
- `PL` - parallel load.  
  If active (Hi)— on clock input rising edge load `Dx` inputs.
- `DS` - serial load input.
- `R` - shifter reset.
  If active (Hi) — clock inputs ignored.

#### Output names:

- `Q`- output

### Parameters

#### Mandatory parameters:

- `size`- size of shifter in rage of [2…64].
- `outPin`- number for Q output in range of [0…size-1].

#### Optional parameters:

- `reverse`- clock inputs sensible to falling edge.
- `plReverse`- `PL` input reversed.
- `clearReverse`- `R` input reversed.
- `inhibitReverse`- `CI` input reversed.

### Example

An 8-bit shifter with `CP`, sensible to falling edge and Q from the highest bit:  
`size=8;outPin=7;reverse`.

