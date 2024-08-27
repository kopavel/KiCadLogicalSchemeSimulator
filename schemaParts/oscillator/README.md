## Oscillator

Implement oscillator.

**Output names**: OUT

**Optional parameter `outName`:** If provided, define output pin name;  
**Optional parameter `freq`:** If provided, define oscillator preset frequency in kilohertz (maybe fractional);  
**Optional parameter `start`:** If provided, oscilator start automatically after net are stabilised;

The schema part is interactive and opens an additional control panel on a click.

![img_1.png](img/oscillator_ui.png)

There are three modes:

- **Manual mode:** Each button press toggles the oscillator output to the opposite state.
- **Automatic mode:** Start at defined frequency. If the frequency set to 0, the process starts 'as fast as possible,' generating new ticks without any
  pause after the current tick processing complete.
- **Processing defined ticks amount:** Useful for debugging where an error occurs after a defined ticks amount, for example CPU containing schemas.

Either contain [oscilloscope](OSCILLOSCOPE.md) functionality;


