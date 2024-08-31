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
package pko.KiCadLogicalSchemeSimulator.components.busDriver;
import pko.KiCadLogicalSchemeSimulator.api.FloatingInException;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

import java.util.Arrays;

public class BusDriver extends SchemaPart {
    private final Bus[] outs;
    private final InBus[] ins;
    private final int partAmount;

    public BusDriver(String id, String sParam) {
        super(id, sParam);
        addOutPin("OUT", false);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        Integer[] sizes = Arrays.stream(params.get("size").split(","))
                .map(Integer::parseInt).toArray(Integer[]::new);
        partAmount = sizes.length;
        ins = new InBus[partAmount];
        outs = new Bus[partAmount];
        for (int i = 0; i < partAmount; i++) {
            ins[i] = addInBus("I" + (char) ('a' + i), sizes[i]);
            addOutBus("O" + (char) ('a' + i), sizes[i]);
            int finalI = i;
            if (reverse) {
                addInPin(new NoFloatingInPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        if (newState) {
                            outs[finalI].setHiImpedance();
                        } else {
                            if (ins[finalI].hiImpedance) {
                                throw new FloatingInException(outs[finalI]);
                            }
                            outs[finalI].state = ins[finalI].state;
                            outs[finalI].setState(ins[finalI].state);
                        }
                    }
                }).state = true;
            } else {
                addInPin(new NoFloatingInPin("OE" + (char) ('a' + i), this) {
                    @Override
                    public void setState(boolean newState) {
                        if (newState) {
                            outs[finalI].state = ins[finalI].state;
                            outs[finalI].setState(ins[finalI].state);
                        } else {
                            outs[finalI].setHiImpedance();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < partAmount; i++) {
            outs[i] = getOutBus("O" + (char) ('a' + i));
        }
    }
}