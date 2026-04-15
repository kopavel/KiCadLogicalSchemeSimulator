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
