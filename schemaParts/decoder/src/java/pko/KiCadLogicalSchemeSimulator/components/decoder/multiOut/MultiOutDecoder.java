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
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class MultiOutDecoder extends SchemaPart {
    public final Part[] parts;
    public MultiOutDecoderABus aBus;
    int partAmount;
    int outSize;

    protected MultiOutDecoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        if (!params.containsKey("cs")) {
            throw new RuntimeException("Component " + id + " has no parameter \"cs\"");
        }
        String[] partCSs = params.get("cs").split(",");
        partAmount = partCSs.length;
        parts = new Part[partAmount];
        outSize = (int) Math.pow(2, inSize);
        aBus = addInBus(new MultiOutDecoderABus("A", this, inSize));
        for (int partNo = 0; partNo < partAmount; partNo++) {
            Part part = (parts[partNo] = new Part());
            part.outs = new Pin[outSize];
            String[] csItems = partCSs[partNo].split(":");
            part.CSs = new boolean[csItems.length];
            part.csPins = new MultiOutDecoderCsPin[csItems.length];
            for (int csNo = 0; csNo < csItems.length; csNo++) {
                part.CSs[csNo] = "R".equals(csItems[csNo]);
                part.csPins[csNo] = addInPin(new MultiOutDecoderCsPin("CS" + ((char) ('a' + partNo)) + csNo, part, csNo, this));
                if (!part.CSs[csNo]) {
                    part.csState |= 1 << csNo;
                }
            }
            for (int outNo = 0; outNo < outSize; outNo++) {
                if (params.containsKey("openCollector")) {
                    addTriStateOutPin("Q" + ((char) ('a' + partNo)) + outNo);
                } else {
                    addOutPin("Q" + ((char) ('a' + partNo)) + outNo);
                }
            }
        }
    }

    @Override
    public void initOuts() {
        for (int partNo = 0; partNo < partAmount; partNo++) {
            Part part = parts[partNo];
            for (int outNo = 0; outNo < outSize; outNo++) {
                part.outs[outNo] = getOutPin("Q" + ((char) ('a' + partNo)) + outNo);
                if (params.containsKey("openCollector") && part.csState > 0) {
                    part.outs[outNo].hiImpedance = true;
                }
            }
        }
    }

    public static class Part {
        public Pin[] outs;
        public MultiOutDecoderCsPin[] csPins;
        public boolean[] CSs;
        public int csState;
    }
}
