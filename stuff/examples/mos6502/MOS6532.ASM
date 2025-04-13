@0x00                   
                            
RIOT_BASE = 0xC000
RIOT_1024_TIMER = 0xC000+0x1f
RIOT_1_TIMER = 0xC000+0x1C

ORIGIN = 0xE000

==Start==
LDA #0xff
STA RIOT_BASE+3
LDX #0x00
LDY #0xff
STY RIOT_1024_TIMER
{
 JMP RE
}

==NMI_handler==
INX
STX RIOT_BASE+2
STY RIOT_1024_TIMER
RTI
==Irq_handler==
RTI

@0xFFFA
DATA NMI_handler
DATA Start
DATA Irq_handler