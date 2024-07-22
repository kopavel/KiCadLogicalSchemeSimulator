/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
        this.initState = initAcquiredState;
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
