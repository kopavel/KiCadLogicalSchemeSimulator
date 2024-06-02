package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.groups;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;

public class SameMaskGroupPins extends MaskGroupPins {
    public SameMaskGroupPins(MaskGroupPin oldItem) {
        super(oldItem);
    }

    public void onChange(long newState, boolean hiImpedance) {
        if (oldVal != newState || oldImpedance != hiImpedance) {
            oldVal = newState;
            oldImpedance = hiImpedance;
            for (InPin inPin : dest) {
                inPin.state = newState;
                inPin.onChange(newState, hiImpedance);
            }
        }
    }

    public void onChange(long newState) {
        if (oldVal != newState) {
            oldVal = newState;
            for (InPin inPin : dest) {
                inPin.state = newState;
                inPin.onChange(newState, false);
            }
        }
    }
}
