package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.groups;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MaskGroupPins extends MaskGroupPin {
    public InPin[] dest;

    public MaskGroupPins(MaskGroupPin oldItem) {
        super(oldItem.dest);
        this.dest = new InPin[]{oldItem.dest};
    }

    public void addDest(InPin pin) {
        dest = Utils.addToArray(dest, pin);
    }

    public void onChange(long newState, boolean hiImpedance) {
        long maskState = newState & mask;
        if (oldVal != maskState || oldImpedance != hiImpedance) {
            oldVal = maskState;
            oldImpedance = hiImpedance;
            for (InPin inPin : dest) {
                inPin.state = maskState;
                inPin.onChange(maskState, hiImpedance);
            }
        }
    }

    public void onChange(long newState) {
        long maskState = newState & mask;
        if (oldVal != maskState) {
            oldVal = maskState;
            for (InPin inPin : dest) {
                inPin.state = maskState;
                inPin.onChange(maskState, false);
            }
        }
    }

    @Override
    public void resend(long newState, boolean hiImpedance) {
        RuntimeException result = null;
        long maskState = newState & mask;
        for (InPin inPin : dest) {
            try {
                inPin.state = maskState;
                inPin.onChange(maskState, hiImpedance);
            } catch (FloatingPinException | ShortcutException e) {
                if (result == null) {
                    result = e;
                }
            }
        }
        if (result != null) {
            throw result;
        }
        oldVal = maskState;
        oldImpedance = hiImpedance;
    }
}