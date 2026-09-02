/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.components.tester.TestInBusSpi;
import pko.KiCadLogicalSchemeSimulator.components.tester.TestInPinSpi;
import pko.KiCadLogicalSchemeSimulator.components.tester.TestOutBusSpi;
import pko.KiCadLogicalSchemeSimulator.components.tester.TestOutPinSpi;

module KiCadLogicalSchemeSimulator.components.Test {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides SchemaPartSpi with TestInBusSpi, TestOutBusSpi, TestInPinSpi, TestOutPinSpi;
}