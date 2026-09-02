/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.oscillator.OscillatorSpi;

module KiCadLogicalSchemeSimulator.components.oscillator {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    requires static lombok;
    provides SchemaPartSpi with OscillatorSpi;
}