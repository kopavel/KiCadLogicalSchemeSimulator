/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools;
import lombok.Getter;

@SuppressWarnings("unused")
public class BusyWaitMutex {
    private final boolean initState;
    @Getter
    private volatile boolean acquired;
    private boolean enabled;

    public BusyWaitMutex() {
        this(false, false);
    }

    public BusyWaitMutex(boolean initAcquiredState) {
        this(initAcquiredState, false);
    }

    public BusyWaitMutex(boolean initAcquiredState, boolean useThreadPark) {
        acquired = initAcquiredState;
        initState = initAcquiredState;
    }

    public synchronized void acquire() {
        if (enabled) {
            //noinspection WhileLoopSpinsOnField,StatementWithEmptyBody
            while (acquired) {
            }
            acquired = true;
        }
    }

    public void release() {
        if (enabled) {
            if (acquired) {
                acquired = false;
            } else {
                throw new RuntimeException("Mutex are not acquired");
            }
        }
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean enabled) {
        this.enabled = false;//release busy thread
        acquired = false;
        synchronized (this) {
            acquired = initState;
            this.enabled = enabled;
        }
    }
}
