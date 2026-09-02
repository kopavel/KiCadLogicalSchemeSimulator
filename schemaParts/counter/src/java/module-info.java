/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.counter.CounterSpi;
import pko.KiCadLogicalSchemeSimulator.components.counter.multipart.MultiPartCounterSpi;

open module KiCadLogicalSchemeSimulator.components.counter {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with CounterSpi, MultiPartCounterSpi;
}