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
package pko.KiCadLogicalSchemeSimulator.components.display;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.in.NoFloatingInPin;

import javax.swing.*;
import java.util.Arrays;

public class Display extends SchemaPart implements InteractiveSchemaPart {
    private final InPin vIn;
    private final DisplayUiComponent display;
    private final InPin hSync;
    private final InPin vSync;
    private final Object refresh = new Object();
    public byte[][] ram = new byte[1][4096];
    public int hSize;
    public int vSize;
    int rows;
    double fps;
    //    private byte[] firstRow = new byte[4096];
    private int hPos;
    private int vPos;
    private boolean lastVSync = true;
    private boolean lastHSync = true;

    public Display(String id, String sParam) {
        super(id, sParam);
        try {
            if (params.containsKey("scale")) {
                int scale = Integer.parseInt(params.get("scale"));
                display = new DisplayUiComponent(id, 100, scale, this);
            } else {
                display = new DisplayUiComponent(id, 100, 2, this);
            }
        } catch (NumberFormatException r) {
            throw new RuntimeException("Component " + id + " size must be positive number");
        }
        Thread.ofVirtual().start(() -> {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    SwingUtilities.invokeLater(display::repaint);
                    synchronized (refresh) {
                        refresh.wait();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        if (reverse) {
            addInPin(new NoFloatingInPin("Clock", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state) {
                        if (!vSync.state && !lastVSync) {
                            if (vSize != 0) {
                                synchronized (refresh) {
                                    refresh.notifyAll();
                                }
                            } else {
                                vSize = vPos + 1;
                                Thread.ofVirtual().start(() -> SwingUtilities.invokeLater(() -> {
                                    while (!display.sized) {
                                        try {
                                            //noinspection BusyWait
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    //noinspection deprecation
                                    display.reshape(display.currentX, display.currentY, hSize * display.scaleFactor, vSize * display.scaleFactor);
                                }));
                                ram = Arrays.copyOf(ram, vSize);
                            }
                            hPos = 0;
                            vPos = 0;
                        } else if (!hSync.state && !lastHSync) {
                            if (hSize == 0) {
                                hSize = hPos + 1;
                                byte[] firstRow = Arrays.copyOf(ram[0], hSize);
                                ram = new byte[2048][hSize];
                                ram[0] = firstRow;
                            }
                            hPos = 0;
                            vPos++;
                            rows++;
                        }
                        byte data = (byte) (vIn.state ? 0xff : 0x0);
                        ram[vPos][hPos] = data;
                        hPos++;
                        lastHSync = !hSync.state;
                        lastVSync = !vSync.state;
                    }
                }
            });
        } else {
            addInPin(new NoFloatingInPin("Clock", this) {
                @Override
                public void setState(boolean newState) {
                    state = newState;
                    if (!state) {
                        if (vSync.state && !lastVSync) {
                            if (vSize == 0) {
                                vSize = vPos + 2;
                                SwingUtilities.invokeLater(() -> {
                                    //noinspection deprecation
                                    display.reshape(display.currentX, display.currentY, hSize * display.scaleFactor, vSize * display.scaleFactor);
                                });
                                ram = Arrays.copyOf(ram, vSize);
                            } else {
                                synchronized (refresh) {
                                    refresh.notifyAll();
                                }
                            }
                            hPos = 0;
                            vPos = 0;
                        } else if (hSync.state && !lastHSync) {
                            if (hSize == 0) {
                                hSize = hPos + 2;
                                byte[] firstRow = Arrays.copyOf(ram[0], hSize);
                                ram = new byte[2048][hSize];
                                ram[0] = firstRow;
                            }
                            hPos = 0;
                            vPos++;
                            rows++;
                        }
                        hPos++;
                        ram[vPos][hPos] = (byte) (vIn.state ? 0xff : 0x0);
                        lastHSync = hSync.state;
                        lastVSync = vSync.state;
                    }
                }
            });
        }
        vIn = addInPin("Vin");
        hSync = addInPin("HSync");
        vSync = addInPin("VSync");
    }

    @Override
    public String extraState() {
        fps = (fps * 0.9) + (1.0 * rows / vSize);
        rows = 0;
        return "Width :" + hSize + "\nHeight:" + vSize + "\nFps:" + String.format("%.2f", fps);
    }

    @Override
    public void initOuts() {
    }

    @Override
    public AbstractUiComponent getComponent() {
        return display;
    }

    @Override
    public void reset() {
        hPos = 0;
        vPos = 0;
        lastVSync = true;
        lastHSync = true;
    }
}
