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
package pko.KiCadLogicalSchemeSimulator.components.diode;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Diode extends SchemaPart {
    protected Pin anode;
    protected Pin cathode;

    protected Diode(String id, String sParams) {
        super(id, sParams);
        addPassivePin(new PassivePin("A", this) {
            @Override
            public void onChange() {
                if (otherImpedance || !otherState || (cathode.strengthSensitive && !otherStrong && cathode.strong)) {
                    if (!cathode.hiImpedance) {
                        cathode.setHiImpedance();
                    }
                } else if (cathode.hiImpedance || !cathode.state) {
                    cathode.strong = otherStrong;
                    cathode.setHi();
                }
            }
        });
        addPassivePin(new PassivePin("K", this) {
            @Override
            public void onChange() {
                if (otherImpedance || otherState || (anode.strengthSensitive && !otherStrong && anode.strong)) {
                    if (!anode.hiImpedance) {
                        anode.setHiImpedance();
                    }
                } else if (anode.hiImpedance || anode.state) {
                    anode.strong = otherStrong;
                    anode.setLo();
                }
            }
        });
    }

    @Override
    public void initOuts() {
        anode = getOutPin("A");
        cathode = getOutPin("K");
    }
}
