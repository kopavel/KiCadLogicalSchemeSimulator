## MultiOutDecoder

Implements a binary decoder with multiple output pin groups.

### Pins

#### Input names:

- `Ax` input  
  x - sequential number in range [0…size-1]
- `CSxn` - Chip Select  
  x - a part identifier in the range [a…z]  
  n - a sequential number starting from in range [0…2^size-1].

#### Output names:

- `Qxn` part output
  x - a part identifier in the range [a…z]
  n - a sequential number in range [0…63].

### Parameters

#### Mandatory parameters:

- `size`- amount of `A` pins in range [1…6]
- `cs`- CS pins  
  is a comma-separated list like `<part>,<part>`,
    - `<part>`- a colon-separated list like `N:R:R`.
        - `R`- reverse mode - `Low` active
        - `N`- normal mode - `Hi` active

#### Optional parameters:

- `reverse`- outputs reversed.

### Example

- With two `A` pins: `size=2`.
- With reversed output pins: `reverse`.
- Group A has 2 CS pins, one of which is reversed: `N:R`.
- Group B has 2 CS pins, both of which are reversed: `R:R`.

`size=2;reverse;cs=N:R,R:R`.
