/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.BUF.BufferSpi;
import pko.KiCadLogicalSchemeSimulator.components.BUF.singleBit.SingleBitBufferSpi;

open module KiCadLogicalSchemeSimulator.components.BUF {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with BufferSpi, SingleBitBufferSpi;
}