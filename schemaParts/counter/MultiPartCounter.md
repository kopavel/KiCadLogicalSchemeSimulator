## Multipart binary counter

Multipart binary counter with a defined amount of outputs.

### Pins

#### Input names:

- `Cp`- clock input.  
  p - part ID in range [a…z]  
  sensible to raising edge.
- `Rp`- reset input
  p - part ID in range [a…z]  
  sensible to raising edge.

#### Output names:

- `Qpx`- outputs.  
  p - part ID in range [a…z]  
  x - sequential number in range [0…part_size-1].

### Parameters

#### Mandatory parameters:

- `size`- Pin amount in range [0…63] for each part as coma separated list.

#### Optional parameters:

- `reverse`- `C` and `R` inputs sensible to falling edge.

### Example

Counter with part A has 1 out and part B has 3 outs: `size=1,3`.
 
