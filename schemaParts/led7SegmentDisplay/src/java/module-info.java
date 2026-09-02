/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.led7Segment.Led7SegmentDisplaySpi;

module KiCadLogicalSchemeSimulator.components.led7SegmentDisplay {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires java.desktop;
    provides SchemaPartSpi with Led7SegmentDisplaySpi;
}