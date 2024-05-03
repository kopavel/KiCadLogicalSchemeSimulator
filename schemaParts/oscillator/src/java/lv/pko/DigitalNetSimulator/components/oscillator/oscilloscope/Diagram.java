package lv.pko.DigitalNetSimulator.components.oscillator.oscilloscope;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.pins.out.TriStateOutPin;
import lv.pko.DigitalNetSimulator.tools.UiTools;
import lv.pko.DigitalNetSimulator.tools.ringBuffers.*;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Diagram extends JPanel {
    private static final int BAR_TOP = -4;
    private static final int BAR_MIDDLE = 4;
    private static final int BAR_BOTTOM = 12;
    private final Set<PinItem> pins = new ConcurrentSkipListSet<>();
    double tickSize = 10;
    int offset = 0;

    public Diagram() {
        setBackground(Color.black);
        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            int diagramWidth = Diagram.this.getWidth();
            int oldSize = (int) ((diagramWidth - 10) / tickSize);
            if (e.isControlDown()) {
                if (notches < 0) {
                    tickSize = tickSize / 0.9;
                } else {
                    tickSize = tickSize * 0.9;
                }
                if (tickSize > 40) {
                    tickSize = 40;
                }
                double relPos = 1 - (((double) e.getX()) / diagramWidth);
                int newSize = (int) ((diagramWidth - 10) / tickSize);
                offset += (int) ((oldSize - newSize) * relPos);
            } else {
                offset += oldSize * notches / 30;
            }
            if (offset < 0) {
                offset = 0;
            }
        });
    }

    public void addPin(OutPin out) {
        pins.add(new PinItem(out));
        revalidate();
    }

    public void tick() {
        pins.forEach(item -> item.buffer.put(item.pin instanceof TriStateOutPin triStateOutPin && triStateOutPin.hiImpedance ? -1 : item.pin.state & item.pin.mask));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int size = (int) ((getWidth() - 10) / tickSize);
        int yPos = 10;
        int maxOffset = 0;
        for (PinItem pinItem : pins) {
            double xPos = 10;
            IRingBufferSlice values = pinItem.buffer.take(offset + size + 1, size + 2);
            maxOffset = Math.max(maxOffset, pinItem.buffer.available());
            int start;
            long prevState;
            if (values.size() <= size) {
                start = 0;
                prevState = -1;
            } else {
                start = 1;
                prevState = values.next();
            }
            int end = Math.min(start + size, values.size());
            xPos += tickSize * (size - end + start);
            for (int i = start; i < end; i++) {
                long curState = values.next();
                if (curState == -1) {
                    if (prevState >= 0) {
                        if (pinItem.pin.size > 1) {
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos - 1), yPos + BAR_MIDDLE);
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos - 1), yPos + BAR_MIDDLE);
                        } else {
                            int linePos = yPos + (prevState > 0 ? BAR_TOP : BAR_BOTTOM);
                            g2d.drawLine((int) xPos, linePos, (int) xPos, yPos + BAR_MIDDLE);
                        }
                    }
                    g2d.setColor(Color.blue); // Here you can choose any color
                    g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + tickSize), yPos + BAR_MIDDLE);
                } else {
                    g2d.setColor(Color.green);
                    if (pinItem.pin.size == 1) {
                        int linePos = yPos + (curState > 0 ? BAR_TOP : BAR_BOTTOM);
                        g2d.drawLine((int) xPos, linePos, (int) (xPos + tickSize), linePos);
                        if (curState != prevState) {
                            int begin = (prevState == -1) ? yPos + BAR_MIDDLE : (yPos + (prevState > 0 ? BAR_TOP : BAR_BOTTOM));
                            g2d.drawLine((int) xPos, begin, (int) xPos, linePos);
                        }
                    } else {
                        if (curState != prevState) {
                            if (prevState != -1) {
                                g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos - 1), yPos + BAR_MIDDLE);
                                g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos - 1), yPos + BAR_MIDDLE);
                            }
                            g2d.drawLine((int) (xPos + 2), yPos + BAR_TOP, (int) (xPos + tickSize - 2), yPos + BAR_TOP);
                            g2d.drawLine((int) (xPos + 2), yPos + BAR_BOTTOM, (int) (xPos + tickSize - 2), yPos + BAR_BOTTOM);
                            g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + 1), yPos + BAR_TOP);
                            g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + 1), yPos + BAR_BOTTOM);
                            UiTools.print(curState & pinItem.pin.mask, (int) (xPos + 5), yPos, (int) Math.ceil(pinItem.pin.size / 4f), g2d);
                        } else {
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos + tickSize - 2), yPos + BAR_TOP);
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos + tickSize - 2), yPos + BAR_BOTTOM);
                            if (i == start) {
                                UiTools.print(curState & pinItem.pin.mask, (int) (xPos + 5), yPos, (int) Math.ceil(pinItem.pin.size / 4f), g2d);
                            }
                        }
                    }
                }
                xPos += tickSize;
                prevState = curState;
            }
            yPos += 20;
        }
        if (offset + size + 1 > maxOffset) {
            offset = maxOffset - size - 1;
        }
        if (offset < 0) {
            offset = 0;
        }
    }

    private static final class PinItem implements Comparable<PinItem> {
        private final OutPin pin;
        private final RingBuffer buffer;

        private PinItem(OutPin pin) {
            this.pin = pin;
            if (pin.size < 8) {
                this.buffer = new ByteRingBuffer();
            } else if (pin.size < 16) {
                this.buffer = new ShortRingBuffer();
            } else if (pin.size < 32) {
                this.buffer = new IntRingBuffer();
            } else {
                this.buffer = new LongRingBuffer();
            }
        }

        @Override
        public int compareTo(PinItem o) {
            return this.pin.getName().compareTo(o.pin.getName());
        }
    }
}
