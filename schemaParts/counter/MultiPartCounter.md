## Multipart binary counter

Multipart binary counter with a defined amount of outputs.

### Pins

#### Input names:

- `Cp`- clock input.  
  p - part ID in range [a…z]  
  sensible to raising edge.
- `Rx`- reset inputs  
  x - number in range [0…reset_amount-1]
  sensible to raising edge.  
  multiple pins combined in `AND` principal.

#### Output names:

- `Qpx`- outputs.  
  p - part ID in range [a…z]  
  x - sequential number in range [0…part_size-1].

### Parameters

#### Mandatory parameters:

- `size`- Pin amount in range [0…32] for each part as coma separated list.

#### Optional parameters:

- `skip`- skip mask for each part as coma separated list.  
  if according bits are high - skip one count  
  0 or empty - no skip.
- `resetAmount`- Reset pin amount in range [0…32].
- `reverse`- `C` input sensible to falling edge.
- `resetReverse`- `R` inputs sensible to falling edge.

### Example

Counter with part A has 1 out and part B has 3 outs: `size=1,3`.
 
