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
package lv.pko.KiCadLogicalSchemeSimulator.components.decoder;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Decoder extends SchemaPart {
    private final InPin cs;
    private TriStateOutPin outPin;
    private boolean csState;
    private long outState;

    protected Decoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        if (params.containsKey("outReverse")) {
            addInPin(new InPin("A", this, inSize) {
                @Override
                public void onChange(long newState, boolean hiImpedance) {
                    outState = ~(1L << correctState(newState));
                    if (csState) {
                        if (hiImpedance) {
                            throw new FloatingPinException(this);
                        }
                        outPin.setState(outState);
                    }
                }
            });
        } else {
            addInPin(new InPin("A", this, inSize) {
                @Override
                public void onChange(long newState, boolean hiImpedance) {
                    outState = 1L << correctState(newState);
                    if (csState) {
                        if (hiImpedance) {
                            throw new FloatingPinException(this);
                        }
                        outPin.setState(outState);
                    }
                }
            });
        }
        cs = addInPin(new InPin("CS", this) {
            @Override
            public void onChange(long newState, boolean hiImpedance) {
                csState = (cs.rawState == 1) ^ reverse;
                if (csState) {
                    if (hiImpedance) {
                        throw new FloatingPinException(this);
                    }
                    outPin.setState(outState);
                } else {
                    outPin.setHiImpedance();
                }
            }
        });
        int outSize = (int) Math.pow(2, inSize);
        addTriStateOutPin("Q", outSize);
        csState = reverse;
    }

    @Override
    public void initOuts() {
        outPin = (TriStateOutPin) getOutPin("Q");
        outPin.useBitPresentation = true;
    }
}
