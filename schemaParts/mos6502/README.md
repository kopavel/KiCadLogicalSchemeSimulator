## MOS 6502 CPU

Implement MOS 6502 CPU pin and timing level functionality.

### Pins

#### Input names:

- `Dx`- Data bus.  
  x - sequential number in range of [0…7].
- `~{RDY}`- Ready
- `F0`- Clock.
- `~{S.O.}` - Set overflow flag.
- `~{RES}`- Reset input.  
  Active at 'Low'
- `~{NMI}`- Non-maskable interrupt.  
  Sensible to falling edge.
- `~{IRQ}` - Maskable interrupt.  
  Sensible to falling edge.

#### Output names:

- `Ax`- Address bus.  
  x - sequential number in range of [0…15].
- `Dx`- Data bus.  
  x - sequential number in range of [0…7].
- `R/~{W}`- Read/Write
- `SYNC`- OpCode fetch.
- `F1`- Phase 1 clock.
- `F2`- Phase 2 clock.

### Parameters

- none

### Description

Component serves as a pin-level wrapper that emulates the behavior of the MOS 6502 processor at the hardware level.  
It's based on sources from [SYMON - A 6502 System Simulator](https://github.com/sethm/symon) project for the CPU core emulation.
The core reimplemented for generating IORequest instead of direct reading from 'BUS' for possibility postpone IO request in time for processing hardware layer.  
The emulation accurately represents the timing diagram of the Mos 6502 processor.
