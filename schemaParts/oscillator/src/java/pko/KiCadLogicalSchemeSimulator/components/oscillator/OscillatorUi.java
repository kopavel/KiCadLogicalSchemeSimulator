/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.oscillator;
import pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope.Oscilloscope;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OscillatorUi extends JFrame {
    public final OscillatorUiComponent parent;
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public Oscilloscope oscilloscope;
    JTextField freqTextField;
    JTextField achievedTextField;
    ScheduledFuture<?> scheduled;
    private JButton startButton;
    private JButton stopButton;
    private JPanel panel;
    private JButton oneTickButton;
    private JTextField tickAmount;
    private JButton doTicks;
    private JTextField totalTicks;
    private JButton oscilloscopeButton;

    public OscillatorUi(OscillatorUiComponent parent) {
        this.parent = parent;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setContentPane(panel);
        setLocationRelativeTo(parent); // Center the frame
        setTitle("Oscillator "+ parent.parent.id);
        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                parent.parent.startClock();
            }
        });
        stopButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                parent.parent.stopClock();
            }
        });
        oscilloscopeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                oscilloscope = new Oscilloscope(OscillatorUi.this);
            }
        });
        freqTextField.getDocument().addDocumentListener(new UiTools.TextChangeListener() {
            @Override
            protected void textChanged() {
                String text = freqTextField.getText();
                if (!text.isBlank()) {
                    if (scheduled != null) {
                        scheduled.cancel(false);
                    }
                    scheduled = scheduler.schedule(() -> {
                        try {
                            parent.parent.setClockFreq(Double.parseDouble(freqTextField.getText()));
                            freqTextField.setBackground(new Color(255, 255, 255, 0));
                        } catch (NumberFormatException e) {
                            freqTextField.setBackground(new Color(255, 0, 0, 91));
                        }
                    }, 300, TimeUnit.MILLISECONDS);
                }
            }
        });
        oneTickButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Thread.ofPlatform().start(() -> {
                    try {
                        parent.parent.tick();
                    } catch (Exception ex) {
                        Log.error(OscillatorUi.class, "Error in tick", ex);
                    }
                });
//                stateButton.setText(parent.parent.state ? "1" : "0");
            }
        });
        doTicks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Thread.ofPlatform().start(() -> {
                    if (!tickAmount.getText().isBlank()) {
                        try {
                            int amount = Integer.parseInt(tickAmount.getText());
                            for (int i = 0; i < amount; i++) {
                                parent.parent.tick();
                            }
                        } catch (Exception ex) {
                            Log.error(OscillatorUi.class, "Error in tick", ex);
                        }
                    }
                });
            }
        });
        //noinspection resource
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            SwingUtilities.invokeLater(() -> freqTextField.setText(String.valueOf((int) (parent.parent.getClockFreq() * 1000))));
        }
    }

    private void tick() {
        SwingUtilities.invokeLater(() -> {
            achievedTextField.setText(parent.formatter.format(parent.parent.currentFreq.getOpaque()));
            totalTicks.setText(String.valueOf(parent.parent.ticks));
        });
    }
}
