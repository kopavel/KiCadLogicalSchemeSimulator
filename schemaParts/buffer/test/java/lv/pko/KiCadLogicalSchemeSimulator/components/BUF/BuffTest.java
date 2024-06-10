package lv.pko.KiCadLogicalSchemeSimulator.components.BUF;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BuffTest {
    Buffer buffer;
    InPin dPin;
    TriStateOutPin qPin;
    InPin csPin;

    public BuffTest() {
        buffer = new Buffer("buf", "size=1");
        dPin = buffer.inMap.get("D");
        qPin = (TriStateOutPin) buffer.outMap.get("Q");
        csPin = buffer.inMap.get("~{CS}");
        InPin dest = new InPin("dest", buffer) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        qPin.addDest(dest);
        dPin.mask = 1;
        dest.mask = 1;
        buffer.initOuts();
    }

    @Test
    @DisplayName("Hi CS")
    void noCs() {
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(1, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
        dPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin Q must be hiImpedance");
    }

    @Test
    @DisplayName("Lo CS ans Hi D")
    void trueInput() {
        csPin.onChange(0, false);
        assertFalse(qPin.hiImpedance, "With Lo CS and Hi D pin Q must not be hiImpedance");
        dPin.onChange(1, false);
        assertEquals(1, qPin.state, "With Lo CS and Hi D pin Q must Hi");
    }

    @Test
    @DisplayName("Lo CS ans Lo D")
    void falseInput() {
        csPin.onChange(0, false);
        dPin.onChange(0, false);
        assertFalse(qPin.hiImpedance, "With Lo CS and Hi D pin Q must not be hiImpedance");
        dPin.onChange(1, false);
        assertEquals(1, qPin.state, "With Lo CS and Hi D pin Q must Hi");
    }

    @Test
    @DisplayName("float exception")
    void floatD() {
        csPin.onChange(1, false);
        assertDoesNotThrow(() -> dPin.onChange(1, true), "Floating input must not throw exception with Hi Cs");
        csPin.onChange(0, false);
        assertThrows(FloatingPinException.class, () -> dPin.onChange(1, true), "Floating input must throw exception with Lo Cs");
    }
}
