package lv.pko.KiCadLogicalSchemeSimulator.components.jnCounter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JnCounterTest {
    JnCounter counter;
    InPin cPin;
    InPin ciPin;
    InPin rPin;
    OutPin qPin;
    OutPin coPin;

    public JnCounterTest() {
        initializeCounter();
    }

    private void initializeCounter() {
        counter = new JnCounter("cnt", "size=4");
        counter.initOuts();
        cPin = counter.inMap.get("C");
        ciPin = counter.inMap.get("CI");
        rPin = counter.inMap.get("R");
        qPin = counter.outMap.get("Q");
        coPin = counter.outMap.get("CO");
        InPin dest = new InPin("dest", counter, 4) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        dest.mask = 0xf;
        qPin.addDest(dest);
        InPin coDest = new InPin("coDest", counter, 4) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        coDest.mask = 1;
        coPin.addDest(coDest);
        counter.reset();
    }

    @Test
    @DisplayName("Initial count is zero")
    void initialCountIsZero() {
        assertEquals(1, qPin.state, "Initial count must be 1");
    }

    @Test
    @DisplayName("Count increments on clock signal")
    void countIncrementsOnClock() {
        assertEquals(0, coPin.state, "Carry out must be 0 when count are 1");
        for (int i = 1; i <= 3; i++) {
            cPin.onChange(1, false);
            assertEquals(Math.pow(2, i), qPin.state, "Count should increment on clock signal");
            if (i == 1) {
                assertEquals(0, coPin.state, "Carry out must be 0 when count are " + Math.pow(2, i));
            } else {
                assertEquals(1, coPin.state, "Carry out must be 1 when count are " + Math.pow(2, i));
            }
        }
        cPin.onChange(1, false);
        assertEquals(1, qPin.state, "Count should reset after reaching maximum");
    }

    @Test
    @DisplayName("Reset pin resets the counter")
    void resetPinResetsCounter() {
        for (int i = 1; i <= 3; i++) {
            cPin.onChange(1, false);
        }
        assertEquals(8, qPin.state, "Count should be 8 before reset");
        rPin.onChange(1, false);
        assertEquals(1, qPin.state, "Count should reset on rising edge of reset pin");
    }

    @Test
    @DisplayName("Count does not change on reset pin falling edge")
    void countDoesNotChangeOnResetFallingEdge() {
        cPin.onChange(1, false);
        assertEquals(2, qPin.state, "Count should be 2 before reset");
        rPin.onChange(0, false);
        assertEquals(2, qPin.state, "Count should not change on falling edge of reset pin");
    }

    @Test
    @DisplayName("Count does not increment on clock falling edge")
    void countDoesNotIncrementOnClockFallingEdge() {
        cPin.onChange(1, false);
        assertEquals(2, qPin.state, "Count should be 2 before test");
        cPin.onChange(0, false);
        assertEquals(2, qPin.state, "Count should not increment on falling edge of clock signal");
    }
}