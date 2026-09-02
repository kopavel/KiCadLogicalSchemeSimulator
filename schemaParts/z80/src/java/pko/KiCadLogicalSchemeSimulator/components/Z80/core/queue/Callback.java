/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */

package pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue;
@FunctionalInterface
public interface Callback {
    void accept(int data);
}
