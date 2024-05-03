## Shifter

Implements a bit shifter.

**Input names**:

- Dx, where x a sequential number starting from 0
- WR,
- CLK

**Output names**: Q

**Mandatory parameter `size`:** specifies the number of D input pins.  
**Optional parameter `reverse`:** if provided, the WR input is reversed

CLK input are sensitive to rising edge.

For example, to create a 8-bit shifter with <span style="text-decoration: overline;">WR</span> you would provide the following parameters: `size=8;reverse`.

