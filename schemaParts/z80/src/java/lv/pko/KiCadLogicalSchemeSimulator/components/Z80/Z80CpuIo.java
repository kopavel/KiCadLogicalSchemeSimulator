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
package lv.pko.KiCadLogicalSchemeSimulator.components.Z80;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Z80CpuIo extends SchemaPart {
    protected InPin dIn;
    protected TriStateOutPin dOut;
    protected TriStateOutPin addOut;
    protected InPin waitPin;
    protected OutPin rdPin;
    protected OutPin wrPin;
    protected OutPin mReqPin;
    protected OutPin ioReqPin;
    protected OutPin m1Pin;
    protected OutPin refreshPin;
    protected OutPin haltPin;
    protected int T;
    protected int M;

    public Z80CpuIo(String id, String sParam) {
        super(id, sParam);
    }

    @Override
    public void initOuts() {
        rdPin = getOutPin("~{RD}");
        wrPin = getOutPin("~{WR}");
        mReqPin = getOutPin("~{MREQ}");
        ioReqPin = getOutPin("~{IORQ}");
        m1Pin = getOutPin("~{M1}");
        refreshPin = getOutPin("~{RFSH}");
        haltPin = getOutPin("~{HALT}");
        addOut = (TriStateOutPin) getOutPin("A");
        dOut = (TriStateOutPin) getOutPin("D");
    }
}
