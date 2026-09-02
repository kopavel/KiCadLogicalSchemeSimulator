/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.params.types;
public class PinConfig {
    public final int unitNo;
    public final String pinName;
    public final SymbolConfig symbolConfig;

    public PinConfig(int unitNo, String pinName, SymbolConfig symbolConfig) {
        this.pinName = pinName;
        this.unitNo = unitNo;
        this.symbolConfig = symbolConfig;
    }
}
