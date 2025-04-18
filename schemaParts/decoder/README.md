## Decoder

Implements binary decoder.

### Pins

#### Input names:

- `Ax`- inputs  
  x - a sequential number in range [0…size-2]
- `CS`- chip select

#### Output names:

- Qx - outputs  
  x - sequential number in range [0...2^size-1]

### Parameters

#### Mandatory parameters:

`size`- input pins amount in range [1…6].

#### Optional parameters:

- `reverse`- `CS` input reversed.
- `outReverse`- outputs reversed.
- `decimal`- decimal mode.

### Example

2 input decoder with `CS` and output reversed: `size=2;reverse;outReverse`