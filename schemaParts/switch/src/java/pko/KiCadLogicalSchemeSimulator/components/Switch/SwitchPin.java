package pko.KiCadLogicalSchemeSimulator.components.Switch;
import pko.KiCadLogicalSchemeSimulator.api.ShortcutException;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

public class SwitchPin extends PassivePin {
    public SwitchPin otherPin;

    public SwitchPin(String id, Switch parent) {
        super(id, parent);
    }

    /*Optimiser constructor unroll destination:destinations*/
    public SwitchPin(SwitchPin oldPin, String variantId) {
        super(oldPin, variantId);
        otherPin = oldPin.otherPin;
    }

    @Override
    public void setState(boolean newState) {
        inImpedance = false;
        inState = newState;
        inStrong = source == null || source.strong;
        hiImpedance = false; //in all scenarios
        if (((Switch) parent).toggled && !otherPin.inImpedance) { // second pin meter...
            if (otherPin.inStrong) { // second pin are strong
                if (inStrong) { // both in strong - shortcut
                    if (Net.stabilizing) {
                        Net.forResend.add(this);
                        assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this, otherPin);
                    } else {
                        throw new ShortcutException(this, otherPin);
                    }
                } else { // we are weak - use second pin
                    forwardState(otherPin.inState, true);
                }
            } else if (inStrong) {
                //second pin are weak but we are strong
                forwardState(newState, true);
            } else if (inState != otherPin.inState) {
                //both are in opposite weak - shortcut
                if (Net.stabilizing) {
                    Net.forResend.add(this);
                    assert Log.debug(this.getClass(), "Shortcut on setting pin {}, try resend later", this, otherPin);
                } else {
                    throw new ShortcutException(this, otherPin);
                }
            }
        } else {
            // second pin doesn't meter
            forwardState(newState, otherPin.inStrong);
        }
    }

    @Override
    public void setHiImpedance() {
        assert !inImpedance : "Already in hiImpedance:" + this;
        inImpedance = true;
        if (((Switch) parent).toggled && !otherPin.inImpedance) {
            forwardState(otherPin.inState, otherPin.inStrong);
        } else {
            hiImpedance = true;
            /*Optimiser block noDest*/
            for (Pin destination : destinations) {
                if (!destination.hiImpedance) {
                    destination.setHiImpedance();
                    destination.hiImpedance = true;
                }
            }
            /*Optimiser blockend noDest*/
        }
    }

    public void forwardState(boolean newState, boolean newStrong) {
        state = newState;
        strong = newStrong;
        hiImpedance = false;
        /*Optimiser block noDest*/
        for (Pin destination : destinations) {
            if (destination.state != newState || destination.strong != newStrong || destination.hiImpedance) {
                destination.state = newState;
                destination.strong = newStrong;
                destination.setState(newState);
                destination.hiImpedance = false;
            }
        }
        /*Optimiser blockend noDest*/
    }

    @Override
    public Pin getOptimised() {
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].getOptimised();
        }
        if (getClass() == SwitchPin.class) {
            ClassOptimiser<SwitchPin> optimiser = new ClassOptimiser<>(this).unroll(destinations.length);
            if (destinations.length == 0) {
                optimiser.cut("noDest");
            }
            return optimiser.build();
        } else {
            return this;
        }
    }
}
