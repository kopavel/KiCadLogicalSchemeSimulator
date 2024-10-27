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
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class DcTrigger extends SchemaPart {
    public InPin dPin;
    public DcRPin rPin;
    public DcSPin sPin;
    public DcCRaisingPin cPin;
    public DcCFallingPin ncPin;
    public Pin qOut;
    public Pin iqOut;
    public boolean clockEnabled = true;

    protected DcTrigger(String id, String sParam) {
        super(id, sParam);
        addOutPin("Q", false);
        addOutPin("~{Q}", true);
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
        dPin = addInPin("D");
        rPin = addInPin(new DcRPin("R", this, params.containsKey("setReverse")));
        sPin = addInPin(new DcSPin("S", this, params.containsKey("setReverse")));
        rPin.sPin = sPin;
        if (reverse) {
            ncPin = addInPin(new DcCFallingPin("C", this));
        } else {
            cPin = addInPin(new DcCRaisingPin("C", this));
        }
    }

    @Override
    public void initOuts() {
        qOut = getOutPin("Q");
        iqOut = getOutPin("~{Q}");
        rPin.qOut = qOut;
        rPin.iqOut = iqOut;
        sPin.qOut = qOut;
        sPin.iqOut = iqOut;
        if (reverse) {
            ncPin.qOut = qOut;
            ncPin.iqOut = iqOut;
        } else {
            cPin.qOut = qOut;
            cPin.iqOut = iqOut;
        }
    }

    @Override
    public void reset() {
        if (clockEnabled) {
            qOut.setLo();
            iqOut.setHi();
        }
    }
}
