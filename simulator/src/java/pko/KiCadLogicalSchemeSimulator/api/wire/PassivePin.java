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
package pko.KiCadLogicalSchemeSimulator.api.wire;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.merger.wire.WireMergerWireIn;
import pko.KiCadLogicalSchemeSimulator.net.wire.PassiveIn;

public abstract class PassivePin extends TriStateOutPin {
    public boolean otherImpedance = true;
    public boolean otherState;
    public boolean otherStrong = true;

    protected PassivePin(String id, SchemaPart parent) {
        super(id, parent);
        strong = false;
        strengthSensitive = true;
    }

    public abstract void onChange();

    @Override
    public Pin getOptimised(ModelItem<?> source) {
        if (!(this.source instanceof PassiveIn) && (destinations.length != 1 || !(destinations[0] instanceof WireMergerWireIn))) {
            return new TriStateOutPin(this, "PassiveOut").getOptimised(source);
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised(this);
            }
            split();
            return this;
        }
    }

    public void recalculateOtherState(boolean mergerImpedance, boolean mergerState, int mergerWeakState, boolean mergerStrong) {
        if (mergerImpedance) {
            otherImpedance = true;
        } else if (hiImpedance) {
            //we in impedance - clone merger
            otherState = mergerState;
            otherStrong = mergerStrong;
            otherImpedance = false;
        } else if (strong) {
            //we strong
            if (mergerWeakState == 0) {
                //no other weak
                otherImpedance = true;
            } else {
                //other weak
                otherImpedance = false;
                otherStrong = false;
                otherState = mergerWeakState > 0;
            }
            //we are weak
        } else if (mergerStrong) {
            //has other strong - clone merger
            otherState = mergerState;
            otherStrong = true;
            otherImpedance = false;
        } else if (mergerWeakState == 1 || mergerWeakState == -1) {
            //we only weak on merger - hiImpedance
            otherImpedance = true;
        } else {
            //merger are many weaks - sp state same as we are.
            otherImpedance = false;
            otherStrong = false;
            otherState = mergerState;
        }
        onChange();
    }
}
