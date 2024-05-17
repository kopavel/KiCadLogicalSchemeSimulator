package lv.pko.KiCadLogicalSchemeSimulator.model;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;

public class InPinInterconnect extends InPin {
    private final InPin dest;
    private final long interconnectMask;
    private final long inverseInterconnectMask;

    public InPinInterconnect(InPin dest, long interconnectMask) {
        super(dest.id, dest.parent, dest.size, dest.aliases.keySet().toArray(String[]::new));
        this.dest = dest;
        this.interconnectMask = interconnectMask;
        this.inverseInterconnectMask = ~interconnectMask & mask;
        //FixMe check if shortcut multiple out pins.
    }

    @Override
    public void onChange(long newState, boolean hiImpedance, boolean weak) {
        if ((rawState & interconnectMask) > 0) {
            dest.rawState = newState | interconnectMask;
            dest.onChange(newState | interconnectMask, hiImpedance, weak);
        } else {
            dest.rawState = newState & inverseInterconnectMask;
            dest.onChange(newState & inverseInterconnectMask, hiImpedance, weak);
        }
    }
}
