package lv.pko.KiCadLogicalSchemeSimulator.model.merger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Splitter extends InPin {
    private InPin[] dest;

    public Splitter(InPin oldDest, InPin newPin) {
        super(oldDest.id, oldDest.parent, oldDest.size);
        dest = new InPin[]{oldDest, newPin};
        mask = oldDest.mask;
    }

    @Override
    public void onChange(long newState, boolean hiImpedance) {
        for (InPin inPin : dest) {
            inPin.state = newState;
            inPin.onChange(newState, hiImpedance);
        }
    }

    public void addDest(InPin pin) {
        dest = Utils.addToArray(dest, pin);
    }
}
