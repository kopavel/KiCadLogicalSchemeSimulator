/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.led16Segment.Led16SegmentDisplaySpi;

module KiCadLogicalSchemeSimulator.components.led16SegmentDisplay {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    provides SchemaPartSpi with Led16SegmentDisplaySpi;
}