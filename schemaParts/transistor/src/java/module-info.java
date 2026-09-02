/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.components.transistor.Transistor;

module KiCadLogicalSchemeSimulator.components.transistor {
    requires KiCadLogicalSchemeSimulator.simulator;
    requires KiCadLogicalSchemeSimulator.components.repeater;
    provides NetFilter with Transistor;
}