## Display

Implement monochrome display.

**Input names**: HSync, VSync, Clock, Vin

**Optional parameter `scale`:** represent scale factor of display (if not provided - default are 2).  
**Optional parameter `reverse`:** If provided, the inputs HSync and VSync is sensitive to negative front, positive otherwise.

The schema part is interactive and show video buffer content on the desk.
Contain vidou buffer size autodetect, in that reason first frame doesn't show until get first VSync.
Clock always sensitive to negative front. 


