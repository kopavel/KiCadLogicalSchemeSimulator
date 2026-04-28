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
