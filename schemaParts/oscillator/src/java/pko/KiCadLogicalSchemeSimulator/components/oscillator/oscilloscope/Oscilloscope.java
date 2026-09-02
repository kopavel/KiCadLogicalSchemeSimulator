/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.components.oscillator.OscillatorUi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//ToDo reset history
//ToDo search by pin changes
//ToDo navigate to start/end
//ToDo add 'time' tags
public class Oscilloscope extends JFrame {
    public final Diagram diagram;
    final JPanel watchedItemNamesPanel;
    private final ScheduledExecutorService scheduler;
    private final OscillatorUi oscillatorUi;

    public Oscilloscope(OscillatorUi oscillatorUi) {
        this.oscillatorUi = oscillatorUi;
        setTitle("Oscilloscope "+ oscillatorUi.parent.parent.id);
        setJMenuBar(new OscilloscopeMenu(this));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
                oscillatorUi.oscilloscope = null;
                oscillatorUi.parent.parent.out = ((OscilloscopePin) oscillatorUi.parent.parent.out).wrapped;
                oscillatorUi.parent.parent.restartClock();
                dispose();
            }
        });
        setLayout(new BorderLayout());
        setSize(500, 300);
        watchedItemNamesPanel = new JPanel();
        watchedItemNamesPanel.setLayout(new BoxLayout(watchedItemNamesPanel, BoxLayout.Y_AXIS));
        watchedItemNamesPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(3, 0, 0, 0), watchedItemNamesPanel.getBorder()));
        diagram = new Diagram();
        add(diagram, BorderLayout.CENTER);
        add(watchedItemNamesPanel, BorderLayout.WEST);
        oscillatorUi.parent.parent.out = new OscilloscopePin(oscillatorUi.parent.parent.out, this);
        oscillatorUi.parent.parent.restartClock();
        setVisible(true);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reDraw, 0, 250, TimeUnit.MILLISECONDS);
        reset(true);
    }

    public void addPin(ModelItem<?> pin, String name, boolean out) {
        FixedHeightLabel label = new FixedHeightLabel(name, watchedItemNamesPanel, diagram);
        watchedItemNamesPanel.add(label);
        watchedItemNamesPanel.revalidate();
        watchedItemNamesPanel.repaint();
        //FixMe check if diagram need to be pinBased, not busBased
        diagram.addPin(pin, name, out);
    }

    public void reDraw() {
        SwingUtilities.invokeLater(() -> {
            diagram.revalidate();
            diagram.repaint();
        });
    }

    public void reset(boolean addClock) {
        diagram.clear();
        watchedItemNamesPanel.removeAll();
        if (addClock) {
            watchedItemNamesPanel.add(new FixedHeightLabel("clock", watchedItemNamesPanel, diagram));
            OscilloscopePin out= (OscilloscopePin) oscillatorUi.parent.parent.out;
            diagram.addPin(out, out.getName(), out.wrapped instanceof OutPin);
        }
        diagram.revalidate();
        watchedItemNamesPanel.revalidate();
        watchedItemNamesPanel.repaint();
    }

    private static class FixedHeightLabel extends JPanel {
        private final JPanel parent;
        private final Diagram diagram;
        int movedFromIndex;

        FixedHeightLabel(String text, JPanel parent, Diagram diagram) {
            this.parent = parent;
            this.diagram = diagram;
            setMaximumSize(new Dimension(200, 20));
            setLayout(new BorderLayout());
            add(new JLabel(text), BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    movedFromIndex = parent.getComponentZOrder(FixedHeightLabel.this);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() > 1) {
                        diagram.removePin(movedFromIndex);
                        parent.remove(movedFromIndex);
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point point = e.getPoint();
                    SwingUtilities.convertPointToScreen(point, FixedHeightLabel.this);
                    Point panelPoint = new Point(point);
                    SwingUtilities.convertPointFromScreen(panelPoint, parent);
                    Component componentUnderMouse = parent.getComponentAt(panelPoint);
                    if (componentUnderMouse instanceof FixedHeightLabel && componentUnderMouse != FixedHeightLabel.this) {
                        int targetIndex = parent.getComponentZOrder(componentUnderMouse);
                        swapLabels(movedFromIndex, targetIndex);
                        movedFromIndex = targetIndex;
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(preferredSize.width + 5, 20);
        }

        private void swapLabels(int fromIndex, int toIndex) {
            if (fromIndex > toIndex) {
                int temp = toIndex;
                toIndex = fromIndex;
                fromIndex = temp;
            }
            Component toComponent = parent.getComponent(toIndex);
            parent.remove(toComponent);
            parent.add(toComponent, fromIndex);
            parent.revalidate();
            parent.repaint();
            Diagram.PinItem toPin = diagram.pins.get(toIndex);
            diagram.pins.remove(toPin);
            diagram.pins.add(fromIndex, toPin);
            diagram.revalidate();
        }
    }
}
