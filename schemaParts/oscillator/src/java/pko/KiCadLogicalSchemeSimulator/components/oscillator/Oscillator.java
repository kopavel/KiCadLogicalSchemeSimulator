/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.oscillator;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.lang.invoke.VarHandle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.MAX_PRIORITY;

public class Oscillator extends SchemaPart implements InteractiveSchemaPart {
    final AtomicReference<Double> currentFreq = new AtomicReference<>(0.00d);
    private final OscillatorUiComponent oscillatorUiComponent;
    private final AtomicReference<Boolean> fullSpeedAlive = new AtomicReference<>(false);
    public long ticks;
    public Pin out;
    ScheduledExecutorService scheduler;
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
                    Pin lOut = out;
                    Net lNet = net;
                    try {
                        AtomicReference<Boolean> lSpeed = fullSpeedAlive;
                        while (lSpeed.getOpaque()) {
                            ticks += 20;
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
                            lOut.setHi();
                            lNet.retry();
                            lOut.setLo();
                            lNet.retry();
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
                try {
                    Pin lOut = out;
                    Net lNet = net;
                    long target = Math.min(10000, (long) ((System.currentTimeMillis() - timerStart) * clockFreq * 2) - ticks + tickStart);
                    ticks += target;
                    for (int i = 0; i < target; i++) {
                        if (lOut.state) {
                            lOut.setLo();
                            lNet.retry();
                        } else {
                            lOut.setHi();
                            lNet.retry();
                        }
                    }
                } catch (Throwable e) {
                    Log.error(Oscillator.class, "TickError {}", ticks, e);
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
                while (net == null || net.stabilizing || out == null) {
                    try {
                        Thread.sleep(10);
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
                net.retry();
            } else {
                out.setHi();
                net.retry();
            }
        } catch (Throwable e) {
            Log.error(Oscillator.class, "TickError {}", ticks, e);
        }
    }
}

