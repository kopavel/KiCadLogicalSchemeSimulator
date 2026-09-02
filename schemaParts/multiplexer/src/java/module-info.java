/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.multiplexer.MultiplexerSpi;
import pko.KiCadLogicalSchemeSimulator.components.multiplexer.SingleBitMultiplexerSpi;
import pko.KiCadLogicalSchemeSimulator.components.multiplexer.masked.MaskedMultiplexerSpi;

open module KiCadLogicalSchemeSimulator.components.multiplexer {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with MultiplexerSpi, MaskedMultiplexerSpi, SingleBitMultiplexerSpi;
}