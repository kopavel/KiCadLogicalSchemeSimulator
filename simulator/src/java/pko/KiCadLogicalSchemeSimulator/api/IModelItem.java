/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import java.util.Set;

public interface IModelItem<T> extends Comparable<IModelItem<T>> {
    String getName();
    int getPriority();
    int getSize();
    int getState();
    void setState(int state);
    boolean isHiImpedance();
    boolean isStrong();
    Byte getAliasOffset(String pinName);
    Set<String> getAliases();
    SchemaPart getParent();
    String getId();
    boolean hasTriStateIn();
    IModelItem<T> getOptimised(ModelItem<?> source);
    IModelItem<T> copyState(IModelItem<? extends T> oldItem, ModelItem<?> source);
    boolean isTriState(ModelItem<?> source);
    @SuppressWarnings("unchecked")
    default T getThis() {
        return (T) this;
    }
    void resend();
    void syncState(int integer);
}
