## Power pin

Schema part with a static state on the output.  
Used for a pull-up, pull-down resistors and power rails.

### Pins

#### Input names:

- none

#### Output names:

- `OUT`- output.

### Parameters

#### Mandatory parameters:

- none

#### Optional parameters:

- `hi`- Hi state, Lo otherwise.
- `strong`- power rail, Pull up/down otherwise.

### Example

Power rail: `hi;strong`