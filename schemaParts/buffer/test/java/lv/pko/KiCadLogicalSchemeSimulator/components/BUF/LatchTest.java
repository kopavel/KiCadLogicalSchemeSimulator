package lv.pko.KiCadLogicalSchemeSimulator.components.BUF;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LatchTest {
    final Buffer buffer;
    final InPin dPin;
    final TriStateOutPin qPin;
    final InPin csPin;
    final InPin wrPin;

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
    @DisplayName("Q pin should be in high-impedance state with Hi CS")
    void qPinHighImpedanceWithHighCs() {
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "With Hi CS pin, Q must be in high-impedance state");
    }

    @Test
    @DisplayName("Q pin should remain high-impedance with Hi CS and changing D pin")
    void qPinRemainsHighImpedanceWithHighCsAndChangingD() {
        csPin.onChange(1, false);
        dPin.onChange(1, true);
        assertTrue(qPin.hiImpedance, "With Hi CS pin, Q must remain in high-impedance state when D pin changes");
    }

    @Test
    @DisplayName("Latch should store and read correctly")
    void latchWriteAndRead() {
        csPin.onChange(1, false);
        dPin.state = 1;
        dPin.onChange(1, false);
        wrPin.onChange(0, false);
        csPin.onChange(0, false);
        assertEquals(1, qPin.state, "Q pin should reflect the state of D pin after OE falling edge");
        csPin.onChange(1, false);
        assertTrue(qPin.hiImpedance, "Q pin should be in high-impedance state after OE raising edge");
    }

    @Test
    @DisplayName("Floating D pin should throw FloatingPinException")
    void floatingDPinThrowsException() {
        assertThrows(FloatingPinException.class, () -> csPin.onChange(1, true), "Floating input must throw exception with Hi CS");
    }
}
