/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

@AllArgsConstructor
public class Pins {
    public InPin dPin;
    public Pin qOut;
    public Pin iqOut;
    public Pins(Pin qOut, Pin iqOut) {
        this.qOut = qOut;
        this.iqOut = iqOut;
    }
}
