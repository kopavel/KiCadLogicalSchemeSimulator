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
package pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;
import pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.*;

import javax.swing.*;
import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope.DiagramState.*;
import static pko.KiCadLogicalSchemeSimulator.tools.ringBuffers.RingBuffer.DEFAULT_CAPACITY;

public class Diagram extends JPanel {
    private static final int BAR_TOP = -4;
    private static final int BAR_MIDDLE = 4;
    private static final int BAR_BOTTOM = 12;
    private static final int BAR_HEIGHT = BAR_BOTTOM - BAR_TOP;
    private final Queue<PinItem> pins = new ConcurrentLinkedQueue<>();
    double tickWidth = 10;
    int offset = 0;
    private Color currentColor;

    public Diagram() {
        setBackground(Color.black);
        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            int diagramWidth = Diagram.this.getWidth();
            int oldSize = (int) ((diagramWidth - 10) / tickWidth);
            if (e.isControlDown()) {
                if (notches < 0) {
                    tickWidth = tickWidth / 0.9;
                } else {
                    tickWidth = tickWidth * 0.9;
                }
                if (tickWidth > 40) {
                    tickWidth = 40;
                } else if (diagramWidth / tickWidth > DEFAULT_CAPACITY) {
                    tickWidth = ((double) diagramWidth) / DEFAULT_CAPACITY;
                }
                double relPos = 1 - (((double) e.getX()) / diagramWidth);
                int newSize = (int) ((diagramWidth - 10) / tickWidth);
                offset += (int) ((oldSize - newSize) * relPos);
            } else {
                offset += oldSize * notches / 30;
            }
            if (offset < 0) {
                offset = 0;
            }
        });
    }

    public void addPin(IModelItem out, String name) {
        pins.add(new PinItem(out, name));
        revalidate();
    }

    public void tick() {
        pins.forEach(item -> item.buffer.put(item.pin instanceof Bus bus && bus.hiImpedance ? -1 : item.pin.getState()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        currentColor = null;
        int ticksInDiagramm = (int) ((getWidth() - 10) / tickWidth);
        int yPos = 10;
        int maxOffset = 0;
        for (PinItem pinItem : pins) {
            boolean singlePin = pinItem.pin.getSize() == 1;
            DiagramState prevDiagramState = HiImpedance;
            DiagramState currDiagramState = null;
            IRingBufferSlice values = pinItem.buffer.take(offset + ticksInDiagramm + 1, ticksInDiagramm + 2);
            maxOffset = Math.max(maxOffset, pinItem.buffer.available());
            int start = 0;
            long prevState = -1;
            int end = Math.min(start + ticksInDiagramm, values.size());
            double xPos = 10 + tickWidth * (ticksInDiagramm - end + start);
            double previousX = xPos;
            boolean hasChanges = false;
            for (int i = start; i < end; i++) {
                long curState = 0;
                if (currDiagramState == MultiChange) {
                    values.skip();
                } else {
                    curState = values.next();
                    if (curState != prevState || currDiagramState == null) {
                        if (curState == -1) {
                            currDiagramState = HiImpedance;
                        } else if (singlePin) {
                            currDiagramState = curState > 0 ? Hi : Lo;
                        } else if (currDiagramState != BusChange) {
                            currDiagramState = curState != prevState ? BusChange : BusActive;
                        }
                        if (curState != prevState) {
                            if (hasChanges) {
                                currDiagramState = MultiChange;
                            } else {
                                hasChanges = true;
                            }
                        }
                        prevState = curState;
                    }
                }
                xPos += tickWidth;
                if (xPos - previousX >= 3) {
                    int linePos;
                    int linePos2;
                    switch (currDiagramState) {
                        case Hi:
                            setColor(g2d, Color.green);
                            linePos = yPos + BAR_TOP;
                            g2d.drawLine((int) previousX, linePos, (int) xPos, linePos);
                            if (currDiagramState != prevDiagramState) {
                                g2d.drawLine((int) previousX, linePos, (int) previousX, yPos + BAR_BOTTOM);
                            }
                            break;
                        case Lo:
                            setColor(g2d, Color.green);
                            linePos = yPos + BAR_BOTTOM;
                            g2d.drawLine((int) previousX, linePos, (int) xPos, linePos);
                            if (currDiagramState != prevDiagramState) {
                                g2d.drawLine((int) previousX, yPos + BAR_TOP, (int) previousX, linePos);
                            }
                            break;
                        case HiImpedance:
                            linePos = yPos + BAR_MIDDLE;
                            if (prevDiagramState == BusActive || prevDiagramState == BusChange) {
                                setColor(g2d, Color.green);
                                linePos2 = (int) (previousX + 2);
                                g2d.drawLine((int) previousX, yPos + BAR_TOP, linePos2, linePos);
                                g2d.drawLine((int) previousX, yPos + BAR_BOTTOM, linePos2, linePos);
                                setColor(g2d, Color.blue);
                                g2d.drawLine((int) previousX + 2, linePos, (int) (xPos), linePos);
                            } else {
                                setColor(g2d, Color.blue);
                                g2d.drawLine((int) previousX, linePos, (int) (xPos), linePos);
                            }
                            break;
                        case BusActive:
                            setColor(g2d, Color.green);
                            linePos = yPos + BAR_TOP;
                            g2d.drawLine((int) previousX, linePos, (int) xPos, linePos);
                            linePos = yPos + BAR_BOTTOM;
                            g2d.drawLine((int) previousX, linePos, (int) xPos, linePos);
                            if (prevDiagramState == MultiChange) {
                                UiTools.print(curState, (int) previousX + 5, yPos, (int) Math.ceil(pinItem.pin.getSize() / 4f), g2d);
                            }
                            break;
                        case BusChange:
                            setColor(g2d, Color.green);
                            linePos = yPos + BAR_TOP;
                            linePos2 = yPos + BAR_BOTTOM;
                            g2d.drawLine((int) previousX + 2, linePos, (int) xPos, linePos);
                            g2d.drawLine((int) previousX + 2, linePos2, (int) xPos, linePos2);
                            if (prevDiagramState == HiImpedance) {
                                linePos = yPos + BAR_MIDDLE;
                                linePos2 = (int) (previousX + 2);
                                g2d.drawLine((int) previousX, linePos, linePos2, yPos + BAR_TOP);
                                g2d.drawLine((int) previousX, linePos, linePos2, yPos + BAR_BOTTOM);
                            } else {
                                g2d.drawLine((int) previousX, linePos2, (int) previousX + 2, linePos);
                                g2d.drawLine((int) previousX, linePos, (int) previousX + 2, linePos2);
                            }
                            UiTools.print(curState, (int) previousX + 5, yPos, (int) Math.ceil(pinItem.pin.getSize() / 4f), g2d);
                            break;
                        case MultiChange:
                            setColor(g2d, Color.green);
                            g2d.fillRect((int) previousX, yPos + BAR_TOP, (int) (xPos - previousX) + 1, BAR_HEIGHT);
                            prevState = values.peek();
                    }
                    prevDiagramState = currDiagramState;
                    currDiagramState = null;
                    previousX = xPos;
                    hasChanges = false;
                }
            }
            yPos += 20;
        }
        if (offset + ticksInDiagramm + 1 > maxOffset) {
            offset = maxOffset - ticksInDiagramm - 1;
        }
        if (offset < 0) {
            offset = 0;
        }
    }

    private void setColor(Graphics2D g2d, Color color) {
        if (currentColor != color) {
            currentColor = color;
            g2d.setColor(color);
        }
    }

    private static final class PinItem implements Comparable<PinItem> {
        private final IModelItem pin;
        private final String name;
        private final RingBuffer buffer;

        private PinItem(IModelItem pin, String name) {
            this.pin = pin;
            this.name = name;
            if (pin.getSize() < 8) {
                this.buffer = new ByteRingBuffer();
            } else if (pin.getSize() < 16) {
                this.buffer = new ShortRingBuffer();
            } else if (pin.getSize() < 32) {
                this.buffer = new IntRingBuffer();
            } else {
                this.buffer = new LongRingBuffer();
            }
        }

        @Override
        public int compareTo(PinItem o) {
            return this.name.compareTo(o.name);
        }
    }
}
