/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.components.dipSwitch.DipSwitch;

open module KiCadLogicalSchemeSimulator.components.dipSwiitch {
    requires KiCadLogicalSchemeSimulator.simulator;
    provides NetFilter with DipSwitch;
}