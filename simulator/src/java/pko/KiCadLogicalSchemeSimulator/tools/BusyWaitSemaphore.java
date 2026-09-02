/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools;
@SuppressWarnings("unused")
public class BusyWaitSemaphore {
    private final int initAvailableAmount;
    private final int maxAmount;
    public volatile int availableAmount;
    private boolean enabled = true;

    public BusyWaitSemaphore() {
        this(1, 0, false);
    }

    public BusyWaitSemaphore(int maxAmount) {
        this(maxAmount, 0, false);
    }

    public BusyWaitSemaphore(int maxAmount, int initAvailableAmount, boolean useThreadPark) {
        availableAmount = initAvailableAmount;
        this.initAvailableAmount = initAvailableAmount;
        this.maxAmount = maxAmount;
    }

    public synchronized void acquire() {
        if (enabled) {
            //noinspection WhileLoopSpinsOnField,StatementWithEmptyBody
            while (availableAmount == 0) {
            }
            availableAmount--;
        }
    }

    public void release() {
        if (enabled) {
            if (availableAmount < maxAmount) {
                //noinspection NonAtomicOperationOnVolatileField
                availableAmount++;
            } else {
                throw new RuntimeException("Semaphore reach max size");
            }
        }
    }

    public void reset() {
        reset(true);
    }

    public void reset(boolean enabled) {
        this.enabled = false;//release busy thread
        availableAmount = 1;
        synchronized (this) {
            availableAmount = initAvailableAmount;
            this.enabled = enabled;
        }
    }
}
