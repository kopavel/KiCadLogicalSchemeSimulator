package lv.pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DcTriggerTest {
    private final InPin dPin;
    private final InPin rPin;
    private final InPin sPin;
    private final InPin cPin;
    private final OutPin qOut;
    private final OutPin iqOut;
    DcTrigger trigger;

    public DcTriggerTest() {
        trigger = new DcTrigger("dct", "");
        trigger.initOuts();
        dPin = trigger.inMap.get("D");
        rPin = trigger.inMap.get("R");
        sPin = trigger.inMap.get("S");
        cPin = trigger.inMap.get("C");
        qOut = trigger.outMap.get("Q");
        iqOut = trigger.outMap.get("~{Q}");
        InPin dest = new InPin("dest", trigger) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
            }
        };
        dest.mask = 1;
        qOut.addDest(dest);
        iqOut.addDest(dest);
    }

    @Test
    @DisplayName("default")
    public void defaultState() {
        assertEquals(0, qOut.state, "Default Q state must be 0");
        assertEquals(1, iqOut.state, "Default ~{Q} state must be 1;");
    }

    @Test
    @DisplayName("main states")
    public void mainStates() {
        cPin.state = 1;
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise  with Lo 'D' Q state must remain 0");
        assertEquals(1, iqOut.state, "after C pin raise with Lo 'D' ~{Q} state must remain 1");
        dPin.state = 1;
        dPin.onChange(1, false);
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "after C pin raise with Hi 'D' Q state must be 1");
        assertEquals(0, iqOut.state, "after C pin raise with Hi 'D' ~{Q} state must be 0");
        dPin.state = 0;
        dPin.onChange(0, false);
        cPin.state = 0;
        cPin.onChange(0, false);
        assertEquals(1, qOut.state, "after C pin fall Q state must be preserved");
        assertEquals(0, iqOut.state, "after C pin fall with Hi 'D' ~{Q} state must be preserved");
        cPin.state = 1;
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "after C pin raise with Lo 'D' Q state must be 0");
        assertEquals(1, iqOut.state, "after C pin raise with Lo 'D' ~{Q} state must be 1");
    }

    @Test
    @DisplayName("RS states")
    public void rsStates() {
        sPin.state = 1;
        sPin.onChange(1, false);
        assertEquals(1, qOut.state, "after S pin set to Hi Q state must be 1");
        assertEquals(0, iqOut.state, "after S pin set to Hi 'D' ~{Q} state must be 0");
        dPin.state = 0;
        dPin.onChange(0, false);
        cPin.onChange(1, false);
        assertEquals(1, qOut.state, "with Hi S pin C pin state change must be ignored");
        assertEquals(0, iqOut.state, "with Hi S pin C pin state change must be ignored");
        rPin.state = 1;
        rPin.onChange(1, false);
        assertEquals(1, qOut.state, "after R pin set to Hi with S pin Hi too Q state must be 1");
        assertEquals(1, iqOut.state, "after R pin set to Hi with S pin Hi too ~{Q] state must be 1");
        sPin.state = 0;
        sPin.onChange(0, false);
        assertEquals(0, qOut.state, "after S pin set to Lo with Hi R  Q state must be 0");
        assertEquals(1, iqOut.state, "after S pin set to Lo with Hi R  ~{Q} state must be 0");
        dPin.state = 0;
        dPin.onChange(0, false);
        cPin.onChange(1, false);
        assertEquals(0, qOut.state, "with Hi R pin C pin state change must be ignored");
        assertEquals(1, iqOut.state, "with Hi R pin C pin state change must be ignored");
    }
}
