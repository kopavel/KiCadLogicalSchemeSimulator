package lv.pko.KiCadLogicalSchemeSimulator.components.OR;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrTest {
    OrGate gate;
    InPin inPin;
    OutPin out;

    public void initializeGate(int size) {
        gate = new OrGate("or", "size=" + size);
        gate.initOuts();
        inPin = gate.inMap.get("IN");
        out = gate.outMap.get("OUT");
        InPin dest = new InPin("dest", gate) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        for (int i = 0; i < size; i++) {
            inPin.mask = inPin.mask << 1 | 1;
        }
        dest.mask = 1;
        out.addDest(dest);
    }

    @Test
    @DisplayName("Both input Lo - out Lo")
    public void bothLo() {
        initializeGate(2);
        inPin.onChange(0, false);
        assertEquals(0, out.state, "With no input output needs to be Lo");
    }

    @Test
    @DisplayName("Only one input Hi - out Hi")
    public void oneHi() {
        initializeGate(2);
        inPin.onChange(1, false);
        assertEquals(1, out.state, "With Hi on only one input output needs to be Hi");
    }

    @Test
    @DisplayName("Both input Hi - out Hi")
    public void bothHi() {
        initializeGate(2);
        inPin.onChange(3, false);
        assertEquals(1, out.state, "With Hi on both inputs output needs to be Hi");
    }

    @Test
    @DisplayName("Floating pin exception")
    public void floatPin() {
        initializeGate(2);
        assertThrows(FloatingPinException.class, () -> inPin.onChange(1, true), "Floating input must throw FloatingPinException");
    }

    @Test
    @DisplayName("Multiple input sizes")
    public void multipleInputSizes() {
        for (int size = 1; size <= 5; size++) {
            initializeGate(size);
            long allHi = (1 << size) - 1;
            inPin.onChange(allHi, false);
            assertEquals(1, out.state, "With Hi on all inputs output needs to be Hi for size " + size);
        }
    }

    @Test
    @DisplayName("Boundary condition: Minimum inputs")
    public void boundaryMinInputs() {
        initializeGate(1);
        inPin.onChange(0, false);
        assertEquals(0, out.state, "With single Lo input output needs to be Lo");
        inPin.onChange(1, false);
        assertEquals(1, out.state, "With single Hi input output needs to be Hi");
    }

    @Test
    @DisplayName("Boundary condition: Maximum inputs")
    public void boundaryMaxInputs() {
        initializeGate(64);
        long allHi = -1;
        inPin.onChange(allHi, false);
        assertEquals(1, out.state, "With Hi on all inputs output needs to be Hi for max size");
    }

    @Test
    @DisplayName("All but one input Lo - out Hi")
    public void allButOneHi() {
        initializeGate(5);
        inPin.onChange(2, false);
        assertEquals(1, out.state, "With Hi on all but one input output needs to be Lo");
    }
}
