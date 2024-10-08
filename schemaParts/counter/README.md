## Binary Counter

Binary counter with a defined amount of outputs.

### Pins

#### Input names:

- `C`- clock input.  
  sensible to raising edge.
- `R`- reset input

#### Output names:

- `Qx`- outputs.  
  x - sequential number in range [0…size-1].

### Parameters

#### Mandatory parameters:

- `size`- amount of output pins.

#### Optional parameters:

- `reverse`- `C` input sensible to falling edge.

### Example

4-output counter, sensible to the negative front on the `C` input: `size=4;reverse`.
 
