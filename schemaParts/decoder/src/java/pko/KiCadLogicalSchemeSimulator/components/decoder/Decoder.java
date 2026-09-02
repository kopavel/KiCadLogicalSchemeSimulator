/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.decoder;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Decoder extends SchemaPart {
    public Bus outBus;
    public DecoderABus aBus;
    public DecoderCsPin csPin;

    protected Decoder(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int inSize = Integer.parseInt(params.get("size"));
        aBus = addInBus(new DecoderABus("A", this, inSize));
        csPin = addInPin(new DecoderCsPin("CS", this));
        int outSize = (int) Math.pow(2, inSize);
        addTriStateOutBus("Q", outSize);
        aBus.csState = reverse;
    }

    @Override
    public void initOuts() {
        outBus = getOutBus("Q");
        aBus.outBus = outBus;
        csPin.outBus = outBus;
        outBus.useBitPresentation = true;
        if (reverse) {
            outBus.state = params.containsKey("outReverse") ? (~1) & Utils.getMaskForSize(outBus.size) : 1;
            outBus.hiImpedance = false;
        }
    }
}
