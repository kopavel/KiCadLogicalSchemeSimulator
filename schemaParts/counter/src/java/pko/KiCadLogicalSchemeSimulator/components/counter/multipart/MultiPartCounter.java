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
package pko.KiCadLogicalSchemeSimulator.components.counter.multipart;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.HashMap;
import java.util.Map;

public class MultiPartCounter extends SchemaPart {
    public final MultiPartCIn[] cIns;
    public final Map<String, MultiPartRIn> rIns = new HashMap<>();
    private final Bus[] outBuses;
    private final Pin[] outPins;
    private final int[] sizes;
    public long resetState;

    protected MultiPartCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        String[] sSizes = params.get("size").split(",");
        String[] skip = params.getOrDefault("skip", "").split(",");
        int resetAmount = Integer.parseInt(params.getOrDefault("resetAmount", "1"));
        outBuses = new Bus[sSizes.length];
        outPins = new Pin[sSizes.length];
        sizes = new int[sSizes.length];
        cIns = new MultiPartCIn[sSizes.length];
        for (int i = 0; i < sSizes.length; i++) {
            try {
                sizes[i] = Integer.parseInt(sSizes[i]);
            } catch (NumberFormatException r) {
                throw new RuntimeException("Component " + id + " sizes part No " + i + " must be positive number");
            }
            if (sizes[i] < 1) {
                throw new RuntimeException("Component " + id + " sizes part No " + i + " must be positive number");
            }
            if (sizes[i] == 1) {
                addOutPin("Q" + (char) ('a' + i));
            } else {
                addOutBus("Q" + (char) ('a' + i), sizes[i]);
            }
            int max = Integer.parseInt((skip.length - 1 < i || skip[i].isBlank()) ? "0" : skip[i]);
            if (reverse) {
                cIns[i] = addInPin(new MultiPartCFallingIn("C" + (char) ('a' + i), this, sizes[i], i, max));
            } else {
                cIns[i] = addInPin(new MultiPartCRaisingIn("C" + (char) ('a' + i), this, sizes[i], i, max));
            }
        }
        for (int i = 0; i < resetAmount; i++) {
            rIns.put("R" + i, addInPin(new MultiPartRIn("R" + i, this, params.containsKey("resetReverse"), i)));
        }
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == 1) {
                outPins[i] = getOutPin("Q" + (char) ('a' + i));
                cIns[i].setOut(outPins[i]);
            } else {
                outBuses[i] = getOutBus("Q" + (char) ('a' + i));
                outBuses[i].useBitPresentation = true;
                cIns[i].setOut(outBuses[i]);
            }
        }
        rIns.values().forEach(pin -> {
            if (pin.isHiImpedance() || !pin.state) {
                resetState |= (1L << pin.no);
            }
        });
    }

    @Override
    public void reset() {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == 1) {
                if (outPins[i].state) {
                    outPins[i].setLo();
                }
            } else if (outBuses[i].state > 0) {
                outBuses[i].setState(0);
            }
        }
    }
}
