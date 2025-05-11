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
    public byte[][] ram = new byte[10][4096];
    public int hSize;
    public int vSize;
    int rows;
    byte pixel;
    double fps;
    private int hPos;
    private int vPos;
    private byte[] row;

    public Display(String id, String sParam) {
        super(id, sParam);
        row = ram[vPos];
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
                        hSize = hPos + 3;
                        byte[] firstRow = Arrays.copyOf(ram[0], hSize);
                        ram = new byte[2048][hSize];
                        ram[0] = firstRow;
                    }
                    hPos = 0;
                    row = ram[vPos++];
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
                        ram = Arrays.copyOf(ram, vSize);
                    }
                    hPos = 0;
                    vPos = 0;
                    row = ram[0];
                }
            });
        } else {
            addInPin(new InPin("HSync", this) {
                @Override
                public void setHi() {
                    state = true;
                    if (hSize == 0 && !parent.net.stabilizing) {
                        hSize = hPos + 3;
                        byte[] firstRow = Arrays.copyOf(ram[0], hSize);
                        ram = new byte[2048][hSize];
                        ram[0] = firstRow;
                    }
                    hPos = 0;
                    vPos++;
                    row = ram[vPos];
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
                        ram = Arrays.copyOf(ram, vSize);
                    }
                    hPos = 0;
                    vPos = 0;
                    row = ram[vPos];
                }

                @Override
                public void setLo() {
                    state = false;
                }
            });
        }
        addInPin(new InPin("Vin", this) {
            @Override
            public void setHi() {
                state = true;
                pixel = (byte) 0xff;
            }

            @Override
            public void setLo() {
                state = false;
                pixel = 0;
            }
        }).hiImpedance = false;
        addInPin(new InPin("Clock", this) {
            @Override
            public void setHi() {
                state = true;
            }

            @Override
            public void setLo() {
                state = false;
                row[hPos++] = pixel;
            }
        });
    }

    @Override
    public String extraState() {
        fps = (fps * 0.9) + ((double) rows / vSize);
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
        ram = new byte[1][4096];
        row = ram[vPos];
    }
}
