package lv.pko.KiCadLogicalSchemeSimulator.components.AND;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NandTest {
    AndGate gate;
    InPin inPin;
    OutPin out;

    public NandTest() {
        gate = new AndGate("AndGate", "size=2;reverse");
        gate.initOuts();
        inPin = gate.inMap.get("IN");
        out = gate.outMap.get("OUT");
        InPin dest = new InPin("dest", gate) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        inPin.mask = 3;
        dest.mask = 1;
        out.addDest(dest);
    }

    @Test
    @DisplayName("Both input Lo - out Hi")
    public void bothLo() {
        inPin.onChange(0, false);
        assertEquals(1, out.state, "With no input output need to be Lo");
    }

    @Test
    @DisplayName("Only one input Hi - out Hi")
    public void oneHi() {
        inPin.onChange(1, false);
        assertEquals(1, out.state, "With Hi on only one input output need to be Lo");
    }

    @Test
    @DisplayName("Both input Hi - out Lo")
    public void bothHi() {
        inPin.onChange(3, false);
        assertEquals(0, out.state, "With Hi on both inputs output need to be Hi");
    }
}
