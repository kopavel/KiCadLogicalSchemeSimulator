## Logical AND gate

### Pins

#### Input names:

- `INx`- Gate input.  
  x - input number in range[0…size-1].

#### Output names:

- `OUT`- Gate output.

### Parameters

#### Mandatory parameters:

- `size`- amount of input pin in range[2…32].

#### Optional parameters:

- `reverse`- reverse output for NAND gate.
- `openColector`- openCollector output

### Example

2-input NAND gate: `size=2;reverse`
