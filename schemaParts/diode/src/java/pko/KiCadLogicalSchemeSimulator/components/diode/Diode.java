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
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class Diode extends SchemaPart {
    private Pin anode;
    private Pin cathode;

    protected Diode(String id, String sParams) {
        super(id, sParams);
        addPassivePin("A");
        addPassivePin("K");
    }

    @Override
    public void initOuts() {
        anode = getOutPin("A");
        cathode = getOutPin("K");
    }

    public void onPassivePinChange(Pin merger) {
        if (anode.merger != merger) {
            setAnode();
        } else {
            setCathode();
        }
    }

    public void setAnode() {
        if (cathode.merger.state || cathode.merger.hiImpedance) {
            if (!anode.hiImpedance) {
                anode.setHiImpedance();
            }
        } else if (cathode.merger.strong && !cathode.strong) {
            if (!anode.strong) {
                anode.strong = true;
                anode.setLo();
            }
        } else if ((cathode.strong || cathode.hiImpedance) ? cathode.merger.weakState != 0 : Math.abs(cathode.merger.weakState) > 1) {
            if (anode.hiImpedance || anode.strong || cathode.merger.weakState > 0) {
                anode.strong = false;
                anode.setLo();
            }
        } else if (!anode.hiImpedance) {
            anode.setHiImpedance();
        }
    }

    public void setCathode() {
        if (!anode.merger.state || anode.merger.hiImpedance) {
            if (!cathode.hiImpedance) {
                cathode.setHiImpedance();
            }
        } else if (anode.merger.strong && !anode.strong) {
            if (!cathode.strong) {
                cathode.strong = true;
                cathode.setHi();
            }
        } else if ((anode.strong || anode.hiImpedance) ? anode.merger.weakState != 0 : Math.abs(anode.merger.weakState) > 1) {
            if (cathode.hiImpedance || cathode.strong || anode.merger.weakState > 0) {
                cathode.strong = false;
                cathode.setHi();
            }
        } else if (!cathode.hiImpedance) {
            cathode.setHiImpedance();
        }
    }
}
