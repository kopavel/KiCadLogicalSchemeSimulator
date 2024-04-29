package lv.pko.DigitalNetSimulator.components.oscillator.oscilloscope;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.pins.out.TriStateOutPin;
import lv.pko.DigitalNetSimulator.tools.RingBuffer;
import lv.pko.DigitalNetSimulator.tools.UiTools;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Diagram extends JPanel {
    private static final int BAR_TOP = -4;
    private static final int BAR_MIDDLE = 4;
    private static final int BAR_BOTTOM = 12;
    private final List<PinItem> pins = new ArrayList<>();
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
        pins.add(new PinItem(out, new RingBuffer<>()));
        revalidate();
    }

    public void tick() {
        pins.forEach(item -> item.buffer.put(new PinState(item.pin.state & item.pin.mask,
                item.pin instanceof TriStateOutPin triStateOutPin && triStateOutPin.hiImpedance)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int size = (int) ((getWidth() - 10) / tickSize);
        int yPos = 10;
        int maxoffset = 0;
        for (PinItem pinItem : pins) {
            double xPos = 10;
            List<PinState> values = pinItem.buffer.take(offset + size + 1, size + 2);
            maxoffset = Math.max(maxoffset, pinItem.buffer.available());
            int start;
            PinState prevState;
            if (values.size() <= size) {
                start = 0;
                prevState = null;
            } else {
                start = 1;
                prevState = values.getFirst();
            }
            int end = Math.min(start + size, values.size());
            xPos += tickSize * (size - end + start);
            for (int i = start; i < end; i++) {
                PinState curState = values.get(i);
                if (curState.hiImpedance) {
                    if (prevState != null && !curState.equals(prevState)) {
                        if (pinItem.pin.size > 1) {
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos - 1), yPos + BAR_MIDDLE);
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos - 1), yPos + BAR_MIDDLE);
                        } else {
                            int linePos = yPos + (prevState.state > 0 ? BAR_TOP : BAR_BOTTOM);
                            g2d.drawLine((int) xPos, linePos, (int) xPos, yPos + BAR_MIDDLE);
                        }
                    }
                    g2d.setColor(Color.blue); // Here you can choose any color
                    g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + tickSize), yPos + BAR_MIDDLE);
                } else {
                    g2d.setColor(Color.green);
                    if (pinItem.pin.size == 1) {
                        int linePos = yPos + (curState.state > 0 ? BAR_TOP : BAR_BOTTOM);
                        g2d.drawLine((int) xPos, linePos, (int) (xPos + tickSize), linePos);
                        if (!curState.equals(prevState)) {
                            int begin = (prevState == null || prevState.hiImpedance) ? yPos + BAR_MIDDLE : (yPos + (prevState.state > 0 ? BAR_TOP : BAR_BOTTOM));
                            g2d.drawLine((int) xPos, begin, (int) xPos, linePos);
                        }
                    } else {
                        if (!curState.equals(prevState)) {
                            if (prevState != null && !prevState.hiImpedance) {
                                g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos - 1), yPos + BAR_MIDDLE);
                                g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos - 1), yPos + BAR_MIDDLE);
                            }
                            g2d.drawLine((int) (xPos + 2), yPos + BAR_TOP, (int) (xPos + tickSize - 2), yPos + BAR_TOP);
                            g2d.drawLine((int) (xPos + 2), yPos + BAR_BOTTOM, (int) (xPos + tickSize - 2), yPos + BAR_BOTTOM);
                            g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + 1), yPos + BAR_TOP);
                            g2d.drawLine((int) xPos, yPos + BAR_MIDDLE, (int) (xPos + 1), yPos + BAR_BOTTOM);
                            UiTools.print(values.get(i).state & pinItem.pin.mask, (int) (xPos + 5), yPos, (int) Math.ceil(pinItem.pin.size / 4f), g2d);
                        } else {
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_TOP, (int) (xPos + tickSize - 2), yPos + BAR_TOP);
                            g2d.drawLine((int) (xPos - 2), yPos + BAR_BOTTOM, (int) (xPos + tickSize - 2), yPos + BAR_BOTTOM);
                            if (i == start) {
                                UiTools.print(values.get(i).state & pinItem.pin.mask, (int) (xPos + 5), yPos, (int) Math.ceil(pinItem.pin.size / 4f), g2d);
                            }
                        }
                    }
                }
                xPos += tickSize;
                prevState = curState;
            }
            yPos += 20;
        }
        if (offset + size + 1 > maxoffset) {
            offset = maxoffset - size - 1;
        }
        if (offset < 0) {
            offset = 0;
        }
        UiTools.print(offset, (int) 10, 200, 6, g2d);
    }

    private record PinItem(OutPin pin, RingBuffer<PinState> buffer) {
    }

    private record PinState(long state, boolean hiImpedance) {
    }
}
