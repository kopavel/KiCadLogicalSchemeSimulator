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
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.net.Net;

import javax.swing.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class Display extends SchemaPart implements InteractiveSchemaPart {
    private final DisplayUiComponent display;
    private final AtomicBoolean refresh = new AtomicBoolean();
    public int hSize;
    public int vSize;
    public int vPos;
    ClockIn clock;
    int rows;
    double fps;

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
        Runnable reshapeRunnable = () -> SwingUtilities.invokeLater(() -> {
            //noinspection deprecation
            display.reshape(display.currentX, display.currentY, hSize * display.scaleFactor, vSize * display.scaleFactor);
        });
        Thread.ofVirtual().start(() -> {
            try {
                Runnable repaintRunnable = display::repaint;
                //noinspection InfiniteLoopStatement
                while (true) {
                    while (!refresh.getOpaque()) {
                        //noinspection BusyWait
                        Thread.sleep(10);
                        Thread.onSpinWait();
                    }
                    refresh.setOpaque(false);
                    SwingUtilities.invokeLater(repaintRunnable);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        if (reverse) {
            addInPin(new InPin("HSync", this) {
                boolean sized = true;

                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (sized && !net.stabilizing) {
                        sized = false;
                        hSize = clock.ramOffset + 3;
                        byte[] oldRam = clock.ram;
                        clock.ram = new byte[2048 * hSize];
                        System.arraycopy(oldRam, 0, clock.ram, 0, hSize);
                    }
                    clock.ramOffset = vPos++ * hSize;
                    rows++;
                }
            });
            addInPin(new InPin("VSync", this) {
                boolean sized;

                @Override
                public void setHi() {
                    state = true;
                }

                @Override
                public void setLo() {
                    state = false;
                    if (sized) {
                        refresh.setOpaque(true);
                    } else if (!net.stabilizing) {
                        vSize = vPos + 2;
                        sized = true;
                        while (!display.sized) {
                            try {
                                //noinspection BusyWait
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Thread.ofVirtual().start(reshapeRunnable);
                        clock.ram = Arrays.copyOf(clock.ram, vSize * hSize);
                    }
                    vPos = 0;
                    clock.ramOffset = 0;
                }
            });
        } else {
            addInPin(new InPin("HSync", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (hSize == 0 && !parent.net.stabilizing) {
                        hSize = clock.ramOffset + 3;
                        byte[] oldRam = clock.ram;
                        clock.ram = new byte[2048 * hSize];
                        System.arraycopy(oldRam, 0, clock.ram, 0, hSize);
                    }
                    clock.ramOffset = vPos++ * hSize;
                    rows++;
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
            addInPin(new InPin("VSync", this) {
                final Net pNet = parent.net;

                @Override
                public void setHi() {
                    state = true;
                    if (vSize != 0) {
                        refresh.setOpaque(true);
                    } else if (!pNet.stabilizing) {
                        vSize = vPos + 2;
                        while (!display.sized) {
                            try {
                                //noinspection BusyWait
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Thread.ofVirtual().start(reshapeRunnable);
                        clock.ram = Arrays.copyOf(clock.ram, vSize * hSize);
                    }
                    vPos = 0;
                    clock.ramOffset = 0;
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        }
        clock = addInPin(new ClockIn(this));
        addInPin(new InPin("Vin", this) {
            @Override
            public void setHi() {
                state = true;
                clock.pixel = (byte) 0xff;
            }

            @Override
            public void setLo() {
                state = false;
                clock.pixel = 0;
            }
        });
    }

    @Override
    public String extraState() {
        if (vSize > 0) {
            fps = (fps * 0.9) + ((double) rows / vSize);
        }
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
        hSize = 0;
        vSize = 0;
        vPos = 0;
        rows = 0;
        fps = 0;
        clock.ram = new byte[4096];
        clock.ramOffset = 0;
    }
}
