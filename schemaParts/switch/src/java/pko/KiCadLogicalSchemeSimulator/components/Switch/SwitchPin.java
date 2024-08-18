package pko.KiCadLogicalSchemeSimulator.components.Switch;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;

public class SwitchPin extends PassivePin {
    public SwitchPin otherPin;

    public SwitchPin(String id, Switch parent) {
        super(id, parent);
    }

    @Override
    public void onChange() {
        if (!((Switch) parent).toggled || otherPin.merger.hiImpedance || (!otherPin.merger.strong && !strong && merger.weakState > 1)) {
            if (!hiImpedance) {
                setHiImpedance();
                hiImpedance = true;
            }
        } else if (otherPin.merger.strong) {
            if (!merger.strong) {
                strong = true;
                setState(state);
            }
        } else {
            strong = false;
            setState(state);
        }
    }
}
