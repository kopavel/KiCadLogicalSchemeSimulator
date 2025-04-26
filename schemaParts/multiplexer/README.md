## Multiplexer

Multiplexer with a defined amount of outputs and ways.

### Pins

#### Input names:

- `Wx`- Data inputs.  
  x - sequential number in range [0…size-1].  
  W - way ID in range [A…ASCII(`A`+((2^nSize-1)-1))].
- `Nx`- way selection inputs.  
  x - sequential number in range [0…((2^nSize-1)-1)].

#### Output names:

- Qw- outputs.  
  w - way ID in range [a…ASCII(`a`+((2^nSize-1)-1))].

### Parameters

#### Mandatory parameters:

- `size`- amount of output pins in one 'way' in range [2…32].
- `nSize`- amount of 'way' identification pins in range [1..5].   
  amount of 'ways' is 2^nSize.

#### Optional parameters:

- none

### Example

4-way 4-pin multiplexer: `size=4;nSize=2`.
