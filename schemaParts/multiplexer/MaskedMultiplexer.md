## Masked Multiplexer

Multiplexer with a defined amount of outputs and ways. Has a multiple OE pint per part.

### Pins

#### Input names:

- `Wx`- Data inputs.  
  x - sequential number in range [0…size-1].  
  W - way ID in range [A…ASCII(`A`+((2^nSize-1)-1))].
- `Nx`- way selection inputs.  
  x - sequential number in range [0…((2^nSize-1)-1)].
- `OE`- outputs enabled.
  `Low` — all outputs in Hi-Impedance.
- `OEw`- way output enabled.
  w - way ID in range [a…ASCII(`a`+((2^nSize-1)-1))].
  `Low` — way output in Hi-Impedance.

#### Output names:

- `Qw`- outputs
  w - way ID in range [a…ASCII(`a`+((2^nSize-1)-1))].

### Parameters

#### Mandatory parameters:

- `size`- amount of output pins in one 'way' in range [2…64].
- `nSize`- amount of 'way' identification pins in range [1..5].   
  amount of 'ways' is 2^nSize.

#### Optional parameters:

- `reverse` — OE inputs reversed.

### Example

4-way 4-pin multiplexer: `size=4;nSize=2`.
