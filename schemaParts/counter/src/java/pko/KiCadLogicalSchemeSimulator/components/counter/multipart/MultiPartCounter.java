/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
    private final Bus[] bOuts;
    private final Pin[] pOuts;
    private final int[] sizes;
    public int resetState;

    protected MultiPartCounter(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        String[] sSizes = params.get("size").split(",");
        String[] skip = params.getOrDefault("skip", "").split(",");
        int resetAmount = Integer.parseInt(params.getOrDefault("resetAmount", "1"));
        bOuts = new Bus[sSizes.length];
        pOuts = new Pin[sSizes.length];
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
                pOuts[i] = getOutPin("Q" + (char) ('a' + i));
                cIns[i].setOut(pOuts[i]);
            } else {
                bOuts[i] = getOutBus("Q" + (char) ('a' + i));
                bOuts[i].useBitPresentation = true;
                cIns[i].setOut(bOuts[i]);
            }
        }
        rIns.values().forEach(pin -> {
            if (pin.isHiImpedance() || !pin.state) {
                resetState |= (1 << pin.no);
            }
        });
    }

    @Override
    public void reset() {
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] == 1) {
                if (pOuts[i].state) {
                    pOuts[i].setLo();
                }
            } else if (bOuts[i].state > 0) {
                bOuts[i].setState(0);
            }
        }
    }
}
