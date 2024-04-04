## Power pin

Implements a schema part with a static state on the output. Can be used for pull-up or pull-down resistors.

**Output names**: OUT

**Mandatory parameter `state`:** Specifies the static OUT state:

- `Lo`: For a low state on the output (preferable to use GND symbol from KiCad).
- `Hi`: For a high state on the output (preferable to use PWR symbol from KiCad).
- `PullUp`: For a "pull-up" state on the output - effectively a resistor connected to the power rail.
- `PullDown`: For a "pull-down" state on the output - effectively a resistor connected to the ground.
- `NC`: Not connected output - does nothing.
