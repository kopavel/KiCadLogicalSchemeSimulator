![img.png](img/z80.png)

**Z80-based Schema Details:**

This schema contains a total about 80 parts, including:

- ~30 pull-up/pull-down resistors
- 20+ OR/NOR/AND/NOT gates
- 5 DC triggers
- 5 counters
- 4 registers
- 4 2-bit 4-way multiplexers
- 2 buffers
- 1 Zilog Z80 CPU
- 1 D-RAM
- 1 S-RAM
- 1 ROM
- 1 oscillator
- 1 decoder
- 1 shifter
- 1 display (providing 512x256 in monochrome)

On an Intel i7-3770s processor, this schema achieved slightly more than 1.6 MHz. Internally, the oscillator frequency is divided by 4 for the CPU, but not for the
video part and memory access, resulting in approximately 1/5 of the physical Z80's usual speed (2 MHz).

I didn't find any reasonable competitor in which I can draw this schema as easily as I can in KiCad, and with decent simulation speed.
