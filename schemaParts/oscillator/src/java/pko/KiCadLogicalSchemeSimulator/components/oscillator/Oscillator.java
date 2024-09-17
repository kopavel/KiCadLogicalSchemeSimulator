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
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.MAX_PRIORITY;

//FixMe sometimes multiple thread started - need make some methods as synchronized. 
public class Oscillator extends SchemaPart implements InteractiveSchemaPart {
    private final OscillatorUiComponent oscillatorUiComponent;
    public long ticks;
    public Pin out;
    ScheduledExecutorService scheduler;
    @Getter
    private double clockFreq = 0;
    private Thread fullSpeedThread;
    private volatile boolean fullSpeedAlive;
    private String outAlias = "OUT";
    private long timerStart;
    private long tickStart;

    public Oscillator(String id, String sParams) {
        super(id, sParams);
        String sAliases = params.get("outName");
        if (sAliases != null) {
            outAlias = sAliases;
        }
        if (params.containsKey("freq")) {
            clockFreq = Double.parseDouble(params.get("freq"));
        }
        addOutPin(outAlias, false);
        oscillatorUiComponent = new OscillatorUiComponent(120, id, this);
    }

    @Override
    public void initOuts() {
        out = getOutPin(outAlias);
    }

    @Override
    public AbstractUiComponent getComponent() {
        return oscillatorUiComponent;
    }

    synchronized public void startClock() {
        if (clockFreq == 0) {
            fullSpeedAlive = true;
            fullSpeedThread = Thread.ofPlatform().priority(MAX_PRIORITY).start(() -> {
                try {
                    while (fullSpeedAlive) {
                        ticks += 2000;
                        for (int i = 0; i < 100; i++) {
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                            out.state = true;
                            out.setState(true);
                            out.state = false;
                            out.setState(false);
                        }
                    }
                } catch (Throwable e) {
                    Log.error(Oscillator.class, "TickError", e);
                }
            });
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            timerStart = System.currentTimeMillis();
            tickStart = ticks;
            long period = Math.max(10, (long) (10000000.0 / clockFreq));
            scheduler.scheduleAtFixedRate(() -> {
                long target = Math.min(10000, (long) ((System.currentTimeMillis() - timerStart) * clockFreq * 2) - ticks + tickStart) / 20;
                ticks += target * 20;
                for (int i = 0; i < target; i++) {
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
                    out.state = true;
                    out.setState(true);
                    out.state = false;
                    out.setState(false);
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
                while (Net.stabilizing) {
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
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        if (fullSpeedThread != null) {
            retVal = true;
            fullSpeedAlive = false;
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
            out.state = !out.state;
            out.setState(out.state);
        } catch (Throwable e) {
            Log.error(Oscillator.class, "TickError", e);
        }
    }
}

