/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.Z80.Z80CpuSpi;

module KiCadLogicalSchemeSimulator.components.Z80Cpu {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires static lombok;
    provides SchemaPartSpi with Z80CpuSpi;
}