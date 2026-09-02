/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

public class Buffer extends SchemaPart {
    public int latch;
    public BufferOePin oePin;
    public BufferCsPin csPin;
    public Bus qBus;
    public BufferDBus dBus;
    public BufferWrPin wrPin;

    public Buffer(String id, String sParam) {
        super(id, sParam);
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int busSize;
        try {
            busSize = Integer.parseInt(params.get("size"));
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        if (busSize < 1) {
            throw new RuntimeException("Component " + id + " size  must be positive number");
        }
        if (busSize > 32) {
            throw new RuntimeException("Component " + id + " size  must be less then 32");
        }
        addTriStateOutBus("Q", busSize);
        if (params.containsKey("latch")) {
            oePin = addInPin(new BufferOePin("OE", this));
            InBus dBus = addInBus("D", busSize);
            wrPin = addInPin(new BufferWrPin("WR", this, dBus, oePin));
        } else {
            dBus = addInBus(new BufferDBus("D", this, busSize));
            csPin = addInPin(new BufferCsPin("CS", this, dBus));
            dBus.csPin = csPin;
        }
    }

    @Override
    public String extraState() {
        return params.containsKey("latch") ? "latch" : "";
    }

    @Override
    public void initOuts() {
        qBus = getOutBus("Q");
        if (params.containsKey("latch")) {
            oePin.qBus = qBus;
            wrPin.qBus = qBus;
        } else {
            csPin.qBus = qBus;
            dBus.qBus = qBus;
        }
        if (reverse) {
            if (qBus.state != 0 || qBus.hiImpedance) {
                qBus.setState(0);
            }
        }
    }

    @Override
    public void reset() {
        latch = 0;
    }
}
