## Display

Monochrome display.  
The schema part is interactive and show video buffer content on the desk.
Contain video buffer size autodetect, in that reason first frame doesn't show until get first VSync.

### Pins

#### Input names:

- `HSync`- Horizontal sync signal.  
  sensitive to raising edge
- `VSync`- Vertical sync signal.  
  sensitive to raising edge
- `Clock`- Clock signal.  
  sensitive to raising edge
- `Vin`- Video input signal.

#### Output names:

- none

### Parameters

#### Mandatory parameters:

- none

#### Optional parameters:

- `scale`- scale factor of display.  
  default are 2.
- `reverse`- Inputs HSync and VSync sensitive to negative front.

