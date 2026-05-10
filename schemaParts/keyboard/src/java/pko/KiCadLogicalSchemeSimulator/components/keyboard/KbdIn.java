package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.Arrays;

public class KbdIn extends InPin {
    OutState[] states = new OutState[0];
    final int mask;
    final int nMask;

    public KbdIn(int no, Keyboard parent) {
        super("In" + no, parent);
        mask = 1 << no;
        nMask = ~mask;
        triStateIn = true;
    }

    public void addState(OutState outState) {
        states = Utils.addToArray(states, outState);
        if (hiImpedance) {
            outState.hi &= nMask;
            outState.lo &= nMask;
        } else if (state) {
            outState.hi |= mask;
            outState.lo &= nMask;
        } else {
            outState.lo |= mask;
            outState.hi &= nMask;
        }
    }

    public void removeState(OutState state) {
        states = Arrays.stream(states)
                .filter(outState -> outState != state).toArray(OutState[]::new);
        state.hi &= nMask;
        state.lo &= nMask;
    }

    @Override
    public void setHi() {
        state = true;
        hiImpedance = false;
        for (OutState state : states) {
            state.hi |= mask;
            state.lo &= nMask;
            state.setOut();
        }
    }

    @Override
    public void setLo() {
        state = false;
        hiImpedance = false;
        for (OutState state : states) {
            state.lo |= mask;
            state.hi &= nMask;
            state.setOut();
        }
    }

    @Override
    public void setHiImpedance() {
        hiImpedance = true;
        for (OutState state : states) {
            state.lo &= nMask;
            state.hi &= nMask;
            state.setOut();
        }
    }
}
