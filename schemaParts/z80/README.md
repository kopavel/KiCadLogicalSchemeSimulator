## Zilog Z80 CPU

Implement Zilog Z80 CPU pin and timing level functionality.

**Input names**:

- CLK
- ~{RESET}
- ~{NMI}
- ~{INT}
- ~{WAIT}
- ~{BUSRQ}

**Output names**:

- Ax, where x is number in [0-15] range
- ~{RD}
- ~{WR}
- ~{MREQ}
- ~{IORQ}
- ~{BUSACK}
- ~{M1}
- ~{RFSH}
- ~{HALT}

**Bidirectional names**:

- Dx, where x is number in [0-7] range

Schema part doesn't have any additional parameters.

<hr>

This component serves as a pin-level wrapper that emulates the behavior of the Z80 processor at the hardware level. It based on
the [Z80Processor](https://github.com/codesqueak/Z80Processor) project for the CPU core emulation.

The core reimplemented in IoQueue/callback manner for possibility postpone IO request in time for processing hardware layer.

The emulation accurately represents the timing diagram of the Z80 processor, with the following exceptions:

- M1 always has 4 T states (plus any additional wait states).
- Subsequent M states have 3 T states each (plus any additional wait states).
- IO reads have 3 T states and, as per Z80 specification, one extra Mw state, resulting in a total of 4 states (plus any additional wait states).

Certain functionalities such as NMI, BUSRQ, BUSACK, and refresh activity are currently absent but are planned for future implementation.

Z80Processor contains information about the real T state amount per OPCODE, allowing for precise execution in terms of T states, although this feature is yet to be
implemented.
