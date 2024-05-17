package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;

public class MaskGroupPin {
    public InPin dest;
    public long mask;
    public long oldVal;
    public boolean oldImpedance = true;

    public MaskGroupPin(InPin dest) {
        this.mask = dest.mask;
        this.dest = dest;
    }

    public void transit(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        if (oldVal != maskState || oldImpedance != hiImpedance) {
            dest.transit(maskState, hiImpedance, false);
            oldVal = maskState;
            oldImpedance = hiImpedance;
        }
    }

    public void resend(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        dest.transit(maskState, hiImpedance, false);
        oldVal = maskState;
        oldImpedance = hiImpedance;
    }
}
