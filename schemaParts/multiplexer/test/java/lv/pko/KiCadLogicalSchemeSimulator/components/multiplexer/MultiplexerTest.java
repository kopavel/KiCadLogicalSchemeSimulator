package lv.pko.KiCadLogicalSchemeSimulator.components.multiplexer;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiplexerTest {
    private final Multiplexer multiplexer;
    private final InPin aPin;
    private final InPin bPin;
    private final InPin nPin;
    private final OutPin qPin;

    public MultiplexerTest() {
        multiplexer = new Multiplexer("mpx", "size=8;nSize=1");
        multiplexer.initOuts();
        aPin = multiplexer.inMap.get("0");
        bPin = multiplexer.inMap.get("1");
        nPin = multiplexer.inMap.get("N");
        qPin = multiplexer.outMap.get("Q");
        InPin qDest = new InPin("qDest", multiplexer) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        qDest.mask = 0xff;
        qPin.addDest(qDest);
    }

    @Test
    @DisplayName("defaultState")
    public void defaultState() {
        assertEquals(0, qPin.state, "default Q state must be 0");
    }

    @Test
    @DisplayName("mutilex test")
    public void multiplexTest() {
        aPin.state = 0x24;
        aPin.onChange(aPin.state, false);
        bPin.state = 0xAC;
        bPin.onChange(bPin.state, false);
        assertEquals(aPin.state, qPin.state, "with n=0 Q state must be equal with A pin state");
        nPin.state = 1;
        nPin.onChange(nPin.state, false);
        assertEquals(bPin.state, qPin.state, "with n=1 Q state must be equal with B pin state");
    }
}
