/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package lv.pko.DigitalNetSimulator.components.Switch;
import lv.pko.DigitalNetSimulator.api.AbstractUiComponent;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.chips.InteractiveChip;
import lv.pko.DigitalNetSimulator.api.pins.PassivePin;
import lv.pko.DigitalNetSimulator.api.pins.PassivePin.OtherState;

//FixME rework it completely for new model
public class Switch extends Chip implements InteractiveChip {
    private final PassivePin pin1;
    private final PassivePin pin2;
    boolean state;
    private SwitchUiComponent switchUiComponent;

    protected Switch(String id, String sParams) {
        super(id, sParams);
        pin1 = addPassivePin(new PassivePin("IN1", id) {
            @Override
            public OtherState otherState() {
                return getOtherState(1);
            }

            @Override
            public void propagate(long state, boolean hiImpedance) {
                propagateState(state, hiImpedance, 1);
            }
        });
        pin2 = addPassivePin(new PassivePin("IN2", id) {
            @Override
            public OtherState otherState() {
                return getOtherState(2);
            }

            @Override
            public void propagate(long state, boolean hiImpedance) {
                propagateState(state, hiImpedance, 2);
            }
        });
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        if (switchUiComponent == null) {
            switchUiComponent = new SwitchUiComponent(this, id);
        }
        return switchUiComponent;
    }

    public void setState(boolean state) {
        this.state = state;
/*
        pin1.reSendState();
        if (!state) {
            pin2.reSendState();
        }
*/
    }

    private OtherState getOtherState(int pinNo) {
        OtherState retVal = new OtherState();
        //        long otherState = pinNo == 1 ? pin1.getState() : pin2.bus.getState();
        //retVal.weak &= ((outPin instanceof PullPin) || (outPin instanceof TriStateOutPin ts && ts.hiImpedance));
            //retVal.value = retVal.value.merge(outPin.state.get(), pin.wire);

        return retVal;
    }

    private void propagateState(long newState, boolean hiImpedance, int pin) {
/*
        Bus bus = pin == 1 ? pin1.bus : pin2.bus;
        long oldState;
        oldState = bus.getState();
        if (oldState == newState) {
            return;
        }
*/
/*
        for (InPin inPin : bus.getInPin()) {
            inPin.onChange(oldState, newState, hiImpedance);
        }
*/
    }
}
