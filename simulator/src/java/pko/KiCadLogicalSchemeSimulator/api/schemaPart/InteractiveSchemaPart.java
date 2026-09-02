/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.schemaPart;
@FunctionalInterface
public interface InteractiveSchemaPart {
    AbstractUiComponent getComponent();
}
