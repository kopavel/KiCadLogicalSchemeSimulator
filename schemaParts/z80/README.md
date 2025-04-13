## Zilog Z80 CPU

Implement Zilog Z80 CPU pin and timing level functionality.

### Pins

#### Input names:

- `Dx`- Data Bus.  
  x - sequential number in range of [0…7].
- `CLK`- Clock.
- `~{RESET}`- Reset input.  
  Active at 'Low'
- `~{NMI}`- Non-maskable interrupt.  
  Sensible to falling edge.
- `~{INT}` - Maskable interrupt.  
  Active at 'Low'.  
  Not implemented yet.
- `~{WAIT}` - Wait.  
  Active at 'Low'
- `~{BUSRQ}`- Bus Request.  
  Active at 'Low'.  
  Not implemented yet.

#### Output names:

- `Dx`- Data Bus.  
  x - sequential number in range of [0…7].
- `Ax`- Address Bus.  
  x - sequential number in range of [0…15].
- `~{RD}`- Read request.  
  Active at 'Low'.
- `~{WR}`- Write request.  
  Active at 'Low'.
- `~{MREQ}`- Memory request.  
  Active at 'Low'.
- `~{IORQ}`- Input/Output port request.  
  Active at 'Low'.
- `~{BUSACK}`- Bus Acknowledge output.  
  Active at 'Low'.
  Not implemented yet.
- `~{M1}`- Machine Cycle One.  
  Active at 'Low'.
- `~{RFSH}`- Refresh.  
  Active at 'Low'.
- `~{HALT}`- HALT State.  
  Active at 'Low'.

### Parameters

- none

### Description

This component serves as a pin-level wrapper that emulates the behavior of the Z80 processor at the hardware level.  
It is based on sources from [Z80Processor](https://github.com/codesqueak/Z80Processor) project for the CPU core emulation.
The core reimplemented for generating IORequest instead of direct reading from 'BUS' for possibility postpone IO request in time for processing hardware layer.  
The emulation accurately represents the timing diagram of the Z80 processor, with the following exceptions:

- M1 always has 4 T states (plus any additional wait states).
- Later M states have 3 T states each (plus any additional wait states).
- IO reads have 3 T states and, as per Z80 specification, one extra Mw state, resulting in
  a total of 4 states (plus any additional wait states).

Certain functionalities, such as INT, BUSRQ, BUSACK, and refresh activity absent, but planned for future implementation.
Z80Processor project contains information about the real T state amount per OP-CODE, allowing for precise execution in terms of T states, although this feature is
yet to be
implemented.
