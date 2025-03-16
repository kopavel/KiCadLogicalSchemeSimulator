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
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin.State;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import static pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin.State.HiImp;

public class Resister extends SchemaPart {
    private Pin in1;
    private Pin in2;

    protected Resister(String id, String sParams) {
        super(id, sParams);
        addPassivePin("IN1");
        addPassivePin("IN2");
    }

    @Override
    public void initOuts() {
        in1 = getOutPin("IN1");
        in2 = getOutPin("IN2");
    }

    public void onPassivePinChange(Pin merger) {
        if (in1.merger != merger) {
            setState(in1, in2);
        } else {
            setState(in2, in1);
        }
    }

    public void setState(Pin in1, Pin in2) {
        State otherState1 = in1.getOtherState();
        State otherState2 = in2.getOtherState();
        if (otherState2 == HiImp || otherState1.strong || (otherState1 != HiImp && otherState2.state == otherState1.state)) {
            in1.setHiImpedance();
        } else if (otherState2.state) {
            in1.setHi();
        } else {
            in1.setLo();
        }
    }
}
