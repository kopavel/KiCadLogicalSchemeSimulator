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

    public void onChange(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        if (oldVal != maskState || oldImpedance != hiImpedance) {
            dest.state = maskState;
            dest.onChange(maskState, hiImpedance);
            oldVal = maskState;
            oldImpedance = hiImpedance;
        }
    }

    public void onChange(long newState) {
        long maskState = newState & mask;
        if (oldVal != maskState) {
            dest.state = maskState;
            dest.onChange(maskState, false);
            oldVal = maskState;
        }
    }

    public void resend(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        dest.state = maskState;
        dest.onChange(maskState, hiImpedance);
        oldVal = maskState;
        oldImpedance = hiImpedance;
    }
}
