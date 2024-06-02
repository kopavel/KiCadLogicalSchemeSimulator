package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.groups;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;

public class SameMaskGroupPin extends MaskGroupPin {
    public SameMaskGroupPin(InPin dest) {
        super(dest);
    }

    public void onChange(long newState, boolean hiImpedance) {
        if (oldVal != newState || oldImpedance != hiImpedance) {
            oldVal = newState;
            oldImpedance = hiImpedance;
            dest.state = newState;
            dest.onChange(newState, hiImpedance);
        }
    }

    public void onChange(long newState) {
        if (oldVal != newState) {
            oldVal = newState;
            dest.state = newState;
            dest.onChange(newState, false);
        }
    }
}
