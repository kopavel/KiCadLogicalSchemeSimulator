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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

//FixMe make unittest
public class MultiUnitDcTrigger extends SchemaPart {
    private final int size;
    public MultiUnitDcCPin cPin;
    public MultiUnitDcRPin rPin;
    public boolean clockEnabled = true;

    protected MultiUnitDcTrigger(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " has no parameter \"size\"");
        }
        try {
            size = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        if (size < 2) {
            throw new RuntimeException("MultiUnitDcTrigger component " + id + " size must be >1");
        }
        InPin[] dPin = new InPin[size];
        Pin[] qOut = new Pin[size];
        Pin[] iqOut = new Pin[size];
        for (int i = 0; i < size; i++) {
            dPin[i] = addInPin("D" + (char) ('a' + i));
            addOutPin("Q" + (char) ('a' + i), false);
            addOutPin("~{Q" + (char) ('a' + i) + "}", true);
            qOut[i] = getOutPin("Q" + (char) ('a' + i));
            iqOut[i] = getOutPin("~{Q" + (char) ('a' + i) + "}");
        }
        rPin = addInPin(new MultiUnitDcRPin("R", this, params.containsKey("setReverse"), qOut, iqOut));
        cPin = addInPin(new MultiUnitDcCPin("C", this, dPin, qOut, iqOut));
    }

    @Override
    public void initOuts() {
        for (int i = 0; i < size; i++) {
            cPin.pins[i].qOut = getOutPin("Q" + (char) ('a' + i));
            cPin.pins[i].iqOut = getOutPin("~{Q" + (char) ('a' + i) + "}");
            rPin.pins[i].qOut = cPin.pins[i].qOut;
            rPin.pins[i].iqOut = cPin.pins[i].iqOut;
        }
    }

    @Override
    public void reset() {
        for (int i = 0; i < size; i++) {
            cPin.pins[i].qOut.setLo();
            cPin.pins[i].iqOut.setHi();
        }
    }
}
