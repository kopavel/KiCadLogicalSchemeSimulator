package lv.pko.KiCadLogicalSchemeSimulator.components.counter;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CounterTest {
    Counter counter;
    InPin cPin;
    InPin rPin;
    OutPin qPin;

    public CounterTest() {
        initializeCounter(2);
    }

    private void initializeCounter(int size) {
        counter = new Counter("cnt", "size=" + size);
        cPin = counter.inMap.get("C");
        rPin = counter.inMap.get("R");
        qPin = counter.outMap.get("Q");
        InPin dest = new InPin("dest", counter, size) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        dest.mask = 0;
        for (int i = 0; i < size; i++) {
            dest.mask = (dest.mask << 1) | 1;
        }
        qPin.addDest(dest);
        counter.initOuts();
        counter.reset();
    }

    @Test
    @DisplayName("Initial count is zero")
    void initialCountIsZero() {
        assertEquals(0, qPin.state, "Initial count must be zero");
    }

    @Test
    @DisplayName("Count increments on clock signal")
    void countIncrementsOnClock() {
        for (int i = 1; i <= 3; i++) {
            cPin.onChange(1, false);
            assertEquals(i & 3, qPin.state, "Count should increment on clock signal");
        }
        cPin.onChange(1, false);
        assertEquals(0, qPin.state, "Count should reset after reaching maximum");
    }

    @Test
    @DisplayName("Reset pin resets the counter")
    void resetPinResetsCounter() {
        for (int i = 0; i < 3; i++) {
            cPin.onChange(1, false);
        }
        assertEquals(3, qPin.state, "Count should be 3 before reset");
        rPin.onChange(1, false);
        assertEquals(0, qPin.state, "Count should reset on rising edge of reset pin");
    }

    @Test
    @DisplayName("Count does not change on reset pin falling edge")
    void countDoesNotChangeOnResetFallingEdge() {
        cPin.onChange(1, false);
        assertEquals(1, qPin.state, "Count should be 1 before reset");
        rPin.onChange(0, false);
        assertEquals(1, qPin.state, "Count should not change on falling edge of reset pin");
    }

    @Test
    @DisplayName("Count does not increment on clock falling edge")
    void countDoesNotIncrementOnClockFallingEdge() {
        cPin.onChange(1, false);
        cPin.onChange(0, false);
        assertEquals(1, qPin.state, "Count should not increment on falling edge of clock signal");
    }
}
