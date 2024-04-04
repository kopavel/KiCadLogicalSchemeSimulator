## Binary Counter

Implements a binary counter with a defined number of outputs.

**Input names**: C  
**Output names**: Qx, where x is a sequential number.

**Mandatory parameter `size`:** Specifies the number of output pins.  
**Optional parameter `reverse`:** If provided, the input is reversed (sensitive to negative front, positive otherwise).  
**Optional parameter `aliases`:** If provided, define pin names. Provided in format "pin1:alias|pin2:alias|...".
Available pins are C for counter input, Q for outputs, R for reset input pin.   
If you specify alias with trailing 1 - it's shift numeration beginning from default 0 to 1 (Q1, Q2 and so on)

For example, to describe a 4-output counter sensitive to the negative front on the input, where input name is `CP`,
outputs are `Qx` starting numeration from 1 and reset pin are `MR` you would provide the following parameters: `size=4;reverse;aliases=C:CP|Q:Q1|R:MR`.
 
