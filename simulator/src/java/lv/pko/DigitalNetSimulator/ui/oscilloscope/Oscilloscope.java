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
package lv.pko.DigitalNetSimulator.ui.oscilloscope;
import lv.pko.DigitalNetSimulator.Simulator;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.schemaPart.InteractiveSchemaPart;
import lv.pko.DigitalNetSimulator.api.schemaPart.SchemaPart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Oscilloscope extends JFrame {
    public static int historySize = 100000;
    Map<String, WatchedItem> items = new ConcurrentHashMap<>();
    JPanel watchedItemNamesPanel;
    JPanel watchedItemsPanel;
    JScrollPane scrollPane;
    ScheduledExecutorService scheduler;

    public Oscilloscope() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                unloadPins();
                scheduler.shutdown();
                setVisible(false);
            }
        });
        setLayout(new BorderLayout());
        setSize(new Dimension(500, 300));
        watchedItemNamesPanel = new JPanel();
        watchedItemNamesPanel.setLayout(new BoxLayout(watchedItemNamesPanel, BoxLayout.Y_AXIS));
        watchedItemNamesPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(3, 0, 0, 0), watchedItemNamesPanel.getBorder()));
        watchedItemsPanel = new JPanel();
        watchedItemsPanel.setLayout(new BoxLayout(watchedItemsPanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(watchedItemsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(watchedItemNamesPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void bringUp() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reDraw, 0, 1, TimeUnit.SECONDS);
        Simulator.oscilloscope.setVisible(true);
        Simulator.oscilloscope.loadPins();
    }

    public void tick() {
        items.values().forEach(WatchedItem::tick);
    }

    public void loadPins() {
        // Example: Load pins from Simulator
        SwingUtilities.invokeLater(() -> {
            for (SchemaPart component : Simulator.model.schemaParts.values()) {
                for (OutPin pin : component.outMap.values()) {
                    connectToPin(pin);
                    if (component instanceof InteractiveSchemaPart) {
//                        pin.bus = new OscilloscopePin(pin.bus);
                    }
                }
/* FixMe
                for (PassivePin pin : component.passiveMap.values()) {
                    if (component instanceof InteractiveSchemaPart) {
                        pin.bus = new OscilloscopeBus(pin.bus);
                    }
                }
*/
            }
        });
    }

    public void connectToPin(OutPin pin) {
        WatchedItem watchedItem = new WatchedItem(pin);
        items.put(pin.getName(), watchedItem);
        watchedItemNamesPanel.add(new FixedHeightLabel(pin.getName()));
        watchedItemsPanel.add(watchedItem);
        watchedItemNamesPanel.revalidate();
        watchedItemsPanel.revalidate();
    }

    public void disconnectFromPin(OutPin pin) {
        /*
            if (pin.bus instanceof OscilloscopePin) {
                pin.bus = ((OscilloscopePin) pin.bus).wrapped;
            }
*/
        items.remove(pin.getName());
    }

    public void reDraw() {
        SwingUtilities.invokeLater(() -> {
            items.values().forEach(WatchedItem::update);
            scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getMaximum());
        });
    }

    private void unloadPins() {
        items.values()
                .stream()
                .map(e -> e.pin).forEach(this::disconnectFromPin);
    }

    private static class FixedHeightLabel extends JPanel {
        public FixedHeightLabel(String text) {
            setMaximumSize(new Dimension(200, 20));
            setLayout(new BorderLayout());
            add(new JLabel(text), BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(preferredSize.width + 5, 20);
        }
    }
}
