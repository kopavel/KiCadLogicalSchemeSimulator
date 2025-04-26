## Bus Driver

Multipart bus driver (buffer) with a defined pins amount in each part.

### Pins

#### Input names:

- `OEx`- Output enabled  
  x - specific part number in range [a…z].
- `Ixn`- inputs  
  x - part name in range [a…z].  
  n - specific part input number in range [0…partSize-1].

#### Output names:

- `Oxn`- outputs  
  x - part name in range [a…z].  
  n - specific part input number in range [0…partSize-1].

### Parameters

#### Mandatory parameters:

- `size`- Pin amount in range [0…32] for each part as coma separated list.

#### Optional parameters:

- `reverse`- reverse `OEx` inputs

### Example

Two part driver, where part `A` has 4 pins and part `B` has 2 pins: `size=4,2`
 
