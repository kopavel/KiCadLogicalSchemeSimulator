/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.dcTrigger.DcTriggerSpi;
import pko.KiCadLogicalSchemeSimulator.components.dcTrigger.multiUnit.MultiUnitDcTriggerSpi;

open module KiCadLogicalSchemeSimulator.components.dcTrigger {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires static lombok;
    provides SchemaPartSpi with DcTriggerSpi, MultiUnitDcTriggerSpi;
}