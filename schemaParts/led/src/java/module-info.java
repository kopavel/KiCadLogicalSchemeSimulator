/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.led.LedSpi;
import pko.KiCadLogicalSchemeSimulator.components.led.indicator.LedIndicatorFilter;
import pko.KiCadLogicalSchemeSimulator.components.led.indicator.LedIndicatorSpi;

module KiCadLogicalSchemeSimulator.schemaParts.led.main {
    exports pko.KiCadLogicalSchemeSimulator.components.led.indicator;
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    requires KiCadLogicalSchemeSimulator.components.Diode;
    provides SchemaPartSpi with LedSpi, LedIndicatorSpi;
    provides NetFilter with LedIndicatorFilter;
}