## MOS 6522 RIOT

Implement MOS 6532 RAM-I/O-Timer (RIOT)

### Pins

#### Input names:

- `Dx`- Data Bus.  
  x - sequential number in range of [0…7].
- `F2`- Phase 2 clock.
- `~{RES}`- Reset input.  
  Active at 'Low'
- `R/~{W}`- Read/Wrire.
- `CS1`- Chip select.
  Active at 'Hi'.
- `~{CS2}`-Chip select.
  Active at 'Low'.
- `~{RS}`-Ram select.
- `PAx`- I/O port input.
  x - sequential number in range of [0…7].
- `PBx`- I/O port input.
  x - sequential number in range of [0…7].

#### Output names:

- `Dx`- Data Bus.  
  x - sequential number in range of [0…7].
- `Ax`- Address Bus.
- `~{IRQ}`- Maskable interrupt.
  x - sequential number in range of [0…6].
- `PAx`- I/O port output.
  x - sequential number in range of [0…7].
- `PBx`- I/O port output.
  x - sequential number in range of [0…7].

### Parameters

- none

### Description

This component serves as a pin-level wrapper that emulates the behavior of the Z80 processor at the hardware level.  
It is based on [fork](https://github.com/kopavel/Z80Processor) from [Z80Processor](https://github.com/codesqueak/Z80Processor) project for the CPU core emulation.
The core reimplemented in IoQueue/callback manner for possibility postpone IO request in time for processing hardware layer.  
The emulation accurately represents the timing diagram of the Z80 processor, with the following exceptions:

- M1 always has 4 T states (plus any additional wait states).
- Later M states have 3 T states each (plus any additional wait states).
- IO reads have 3 T states and, as per Z80 specification, one extra Mw state, resulting in
  a total of 4 states (plus any additional wait states).

Certain functionalities, such as INT, BUSRQ, BUSACK, and refresh activity absent, but planned for future implementation.
Z80Processor project contains information about the real T state amount per OP-CODE, allowing for precise execution in terms of T states, although this feature is
yet to be
implemented.
