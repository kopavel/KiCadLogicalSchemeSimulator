/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.decoder.DecoderSpi;
import pko.KiCadLogicalSchemeSimulator.components.decoder.multiOut.MultiOutDecoderSpi;

open module KiCadLogicalSchemeSimulator.components.decoder {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with DecoderSpi, MultiOutDecoderSpi;
}