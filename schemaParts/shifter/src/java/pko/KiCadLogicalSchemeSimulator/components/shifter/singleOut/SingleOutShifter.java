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
package pko.KiCadLogicalSchemeSimulator.components.shifter.singleOut;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class SingleOutShifter extends SchemaPart {
    public final InBus dBus;
    public final InPin dsPins;
    public final long hiDsMask;
    public final long latchMask;
    public final long outMask;
    final SingleOutShifterRPin rPin;
    final SingleOutShifterCIPin ciPin;
    public long latch = 0;
    public Pin out;
    public boolean parallelLoad;
    public boolean clockEnabled;
    public SingleOutShifterCPin cp;
    public SingleOutShifterCPin cn;

    protected SingleOutShifter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int dSize = Integer.parseInt(params.get("size"));
        if (!params.containsKey("outPin")) {
            throw new RuntimeException("Component " + id + " has no parameter \"outPin\"");
        }
        outMask = 1L << Long.parseLong(params.get("outPin"));
        dBus = addInBus("D", dSize);
        boolean plReverse = params.containsKey("plReverse");
        latchMask = Utils.getMaskForSize(dSize);
        hiDsMask = 1L << (dSize - 1);
        dsPins = addInPin("DS");
        addInPin(new SingleOutShifterPlPin("PL", this, plReverse));
        boolean clearReverse = params.containsKey("clearReverse");
        boolean inhibitReverse = params.containsKey("inhibitReverse");
        clockEnabled = !clearReverse && !inhibitReverse;
        rPin = addInPin(new SingleOutShifterRPin("R", this, inhibitReverse, clearReverse));
        ciPin = addInPin(new SingleOutShifterCIPin("CI", this, inhibitReverse, clearReverse));
        rPin.ciPin = ciPin;
        ciPin.rPin = rPin;
        cp = addInPin(new SingleOutShifterCPin("CP", this, reverse, false));
        cn = addInPin(new SingleOutShifterCPin("CN", this, reverse, true));
        addOutPin("Q", false);
    }

    @Override
    public void initOuts() {
        out = getOutPin("Q");
        cn.out = out;
        cp.out = out;
    }

    @Override
    public String extraState() {
        return "latch" + "\n" + Long.toBinaryString(latch);
    }

    @Override
    public void reset() {
        latch = 0;
        out.setLo();
    }
}
