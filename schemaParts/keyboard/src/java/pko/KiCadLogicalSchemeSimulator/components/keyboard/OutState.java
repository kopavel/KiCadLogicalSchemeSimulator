package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public final class OutState {
    public int hi;
    public int lo;
    public final Pin out;
    private final Pin enabled;

    public OutState(Pin out, Pin enabled) {
        this.out = out;
        this.enabled = enabled;
    }

    public void disable() {
        Pin lOut;
        if (!(lOut = out).hiImpedance) {
            lOut.setHiImpedance();
        }
    }

    public void setOut() {
        if (!enabled.state) {
            if (hi > 0) {
                if (!out.state || out.hiImpedance) {
                    out.setHi();
                }
            } else if (lo > 0) {
                if (out.state || out.hiImpedance) {
                    out.setLo();
                }
            } else if (!out.hiImpedance) {
                out.setHiImpedance();
            }
        }
    }
}
