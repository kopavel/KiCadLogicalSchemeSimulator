package lv.pko.KiCadLogicalSchemeSimulator.components.BUF;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LatchTest {
    Buffer buffer;
    InPin dPin;
    TriStateOutPin qPin;
    InPin csPin;
    InPin wrPin;

    public LatchTest() {
        buffer = new Buffer("buf", "size=1;latch");
        dPin = buffer.inMap.get("D");
        qPin = (TriStateOutPin) buffer.outMap.get("Q");
        csPin = buffer.inMap.get("~{OE}");
        wrPin = buffer.inMap.get("~{WR}");
        InPin dest = new InPin("dest", buffer) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        qPin.addDest(dest);
        dPin.mask = 1;
        dest.mask = 1;
        buffer.initOuts();
        csPin.onChange(1, false);
        wrPin.onChange(1, false);
        buffer.reset();
    }

    @Test
    @DisplayName("Hi CS")
    void noCs() {
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(1, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        wrPin.onChange(0, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(0, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
    }

    @Test
    @DisplayName("Latch store and read")
    void latchWrite() {
        csPin.state = 0;
        csPin.onChange(0, false);
        assertFalse(qPin.hiImpedance, "With Lo CS and Hi D pin Q must not be hiImpedance");
        assertEquals(0, qPin.state, "initial state must be 0");
        dPin.state = 1;
        dPin.onChange(1, false);
        assertEquals(0, qPin.state, "D state must not be taken in account without WR");
        csPin.state = 1;
        csPin.onChange(1, false);
        wrPin.state = 0;
        wrPin.onChange(0, false);
        assertEquals(0, qPin.state, "D state must not be taken in account without WR and OE");
        csPin.state = 0;
        csPin.onChange(0, false);
        assertEquals(1, qPin.state, "Q state mut be 1 as written on WR fall-down edge and OR go Lo");
    }

    @Test
    @DisplayName("float exception")
    void floatD() {
        assertThrows(FloatingPinException.class, () -> csPin.onChange(1, true), "Floating input must throw exception with Lo Cs");
    }
}
