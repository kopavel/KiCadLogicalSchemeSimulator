## Bus Driver

Implements a bus driver with a defined parts amount and pins amount in each part.

**Input names**:

- `OEx` - Output enabled for specific part  
  x — part name in range [a…z].
- `Ixn` - input for specific part  
  x — part name in range [a…z].  
  n — input number in specific part.

**Output names**:

- `Oxn` - output for specific parts  
  x — part name in range [a…z].  
  n — input number in specific part.

**Mandatory parameter: `size`**: Specifies pin amount in each part as coma separated list.  
**Optional parameter `reverse`:** If provided, the `OEx` inputs are reversed.

For example, for two part driver, where part `A` has 4 pins and part `B` has 2 pins provide: `size=4,2`
 
