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
package pko.KiCadLogicalSchemeSimulator.components.oscillator;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.lang.invoke.VarHandle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.MAX_PRIORITY;

public class Oscillator extends SchemaPart implements InteractiveSchemaPart {
    private final OscillatorUiComponent oscillatorUiComponent;
    private final AtomicReference<Boolean> fullSpeedAlive = new AtomicReference<>(false);
    public long ticks;
    public Pin out;
    ScheduledExecutorService scheduler;
    final AtomicReference<Double> currentFreq = new AtomicReference<>(0.00d);
    @Getter
    private double clockFreq;
    private Thread fullSpeedThread;
    private long timerStart;
    private long tickStart;

    public Oscillator(String id, String sParams) {
        super(id, sParams);
        if (params.containsKey("freq")) {
            clockFreq = Double.parseDouble(params.get("freq"));
        }
        addOutPin("OUT", false);
        oscillatorUiComponent = new OscillatorUiComponent(120, id, this);
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
    }

    @Override
    public AbstractUiComponent getComponent() {
        return oscillatorUiComponent;
    }

    synchronized public void startClock() {
        if (clockFreq == 0) {
            if (fullSpeedThread == null || !fullSpeedThread.isAlive()) {
                fullSpeedAlive.setOpaque(true);
                fullSpeedThread = Thread.ofPlatform().priority(MAX_PRIORITY).start(() -> {
                    Pin local = out;
                    try {
                        while (fullSpeedAlive.getOpaque()) {
                            ticks += 20;
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                            local.setHi();
                            local.setLo();
                        }
                    } catch (Throwable e) {
                        Log.error(Oscillator.class, "TickError {}", ticks, e);
                    }
                });
            }
        } else if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            timerStart = System.currentTimeMillis();
            tickStart = ticks;
            int period = Math.max(1, (int) (1000000.0 / clockFreq / 2));
            scheduler.scheduleAtFixedRate(() -> {
                Pin local = out;
                long target = Math.min(10000, (long) ((System.currentTimeMillis() - timerStart) * clockFreq * 2) - ticks + tickStart);
                ticks += target;
                for (int i = 0; i < target; i++) {
                    if (local.state) {
                        local.setLo();
                    } else {
                        local.setHi();
                    }
                }
            }, 0, period, TimeUnit.NANOSECONDS);
        }
    }

    synchronized public void restartClock() {
        if (stopClock()) {
            startClock();
        }
    }

    void startIfDefault() {
        if (fullSpeedThread == null) {
            Thread.ofVirtual().start(() -> {
                while (net.stabilizing || out == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (params.containsKey("start")) {
                    startClock();
                }
            });
        }
    }

    synchronized boolean stopClock() {
        boolean retVal = false;
        if (scheduler != null) {
            retVal = true;
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        if (fullSpeedThread != null) {
            retVal = true;
            fullSpeedAlive.setOpaque(false);
            VarHandle.releaseFence();
            try {
                fullSpeedThread.join();
                fullSpeedThread = null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return retVal;
    }

    void setClockFreq(double clockFreq) {
        this.clockFreq = clockFreq / 1000;
        restartClock();
    }

    void tick() {
        try {
            ticks++;
            if (out.state) {
                out.setLo();
            } else {
                out.setHi();
            }
        } catch (Throwable e) {
            Log.error(Oscillator.class, "TickError {}", ticks, e);
        }
    }
}

