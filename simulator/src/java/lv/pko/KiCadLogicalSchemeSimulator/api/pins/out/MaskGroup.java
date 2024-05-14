package lv.pko.KiCadLogicalSchemeSimulator.api.pins.out;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class MaskGroup {
    public InPin[] dest;
    public long mask;
    public long oldVal;

    public MaskGroup(InPin dest) {
        this.mask = dest.mask;
        this.dest = new InPin[]{dest};
    }

    public void addDest(InPin pin) {
        dest = Utils.addToArray(dest, pin);
    }
}
