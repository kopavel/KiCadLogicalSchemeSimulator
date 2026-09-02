/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.net.merger;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;

import java.util.Collection;

public interface MergerInput<T> extends IModelItem<T> {
    int getMask();
    Collection<MergerInput<?>> getSources();
    void retry();
}
