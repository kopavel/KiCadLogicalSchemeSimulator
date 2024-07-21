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
package lv.pko.KiCadLogicalSchemeSimulator.components.oscillator;
import lombok.Getter;
import lv.pko.KiCadLogicalSchemeSimulator.api.AbstractUiComponent;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.InteractiveSchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//FixMe sometimes multiple thread started - need make some methods as synchronized. 
public class Oscillator extends SchemaPart implements InteractiveSchemaPart {
    private final OscillatorUiComponent oscillatorUiComponent;
    public long ticks;
    public Pin out;
    ScheduledExecutorService scheduler;
    @Getter
    private long clockPeriod = 1000000000;
    private Thread fullSpeedThread;
    private volatile boolean fullSpeedAlive;
    private String outAlias = "OUT";

    public Oscillator(String id, String sParams) {
        super(id, sParams);
        String sAliases = params.get("outName");
        if (sAliases != null) {
            outAlias = sAliases;
        }
        addOutPin(outAlias);
        oscillatorUiComponent = new OscillatorUiComponent(20, id, this);
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
        if (clockPeriod == 0) {
            fullSpeedAlive = true;
            fullSpeedThread = Thread.ofPlatform().start(() -> {
                try {
                    while (fullSpeedAlive) {
                        ticks += 20000;
                        for (int i = 0; i < 1000; i++) {
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                            out.setState(false, true);
                            out.setState(true, true);
                        }
                    }
                } catch (Throwable e) {
                    Log.error(Oscillator.class, "TickError", e);
                }
            });
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::tick, 0, clockPeriod, TimeUnit.NANOSECONDS);
        }
    }

    synchronized public void restartClock() {
        if (stopClock()) {
            startClock();
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

    void setClockPeriod(long period) {
        clockPeriod = period;
        restartClock();
    }

    void tick() {
        try {
            out.state = !out.state;
            out.setState(out.state, true);
        } catch (Throwable e) {
            Log.error(Oscillator.class, "TickError", e);
        }
    }
}

