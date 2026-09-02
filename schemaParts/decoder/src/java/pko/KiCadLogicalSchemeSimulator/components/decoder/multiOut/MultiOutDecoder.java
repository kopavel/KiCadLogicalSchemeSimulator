/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

public class MultiOutDecoder extends SchemaPart {
    public final Part[] parts;
    public MultiOutDecoderABus aBus;
    public boolean hasCs = true;
    int partAmount;
    int outSize;

    protected MultiOutDecoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        String[] partCSs = null;
        if (!params.containsKey("cs")) {
            partAmount = 1;
            hasCs = false;
        } else {
            partCSs = params.get("cs").split(",");
            partAmount = partCSs.length;
        }
        parts = new Part[partAmount];
        outSize = (int) Math.pow(2, inSize);
        aBus = addInBus(new MultiOutDecoderABus("A", this, inSize));
        for (int partNo = 0; partNo < partAmount; partNo++) {
            Part part = (parts[partNo] = new Part());
            part.outs = new Pin[outSize];
            if (partCSs != null) {
                String[] csItems = partCSs[partNo].split(":");
                part.CSs = new boolean[csItems.length];
                part.csPins = new MultiOutDecoderCsPin[csItems.length];
                for (int csNo = 0; csNo < csItems.length; csNo++) {
                    part.CSs[csNo] = "R".equals(csItems[csNo]);
                    part.csPins[csNo] = addInPin(new MultiOutDecoderCsPin("CS" + ((char) ('a' + partNo)) + csNo, part, csNo, this));
                    if (part.CSs[csNo]) {
                        part.csState |= 1 << csNo;
                    }
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
                if (!hasCs || part.csState == 0) {
                    if (outNo == aBus.state && params.containsKey("openCollector")) {
                        part.outs[outNo].hiImpedance = false;
                    } else if (reverse) {
                        part.outs[outNo].state = true;
                    }
                } else if (reverse) {
                    part.outs[outNo].state = true;
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
