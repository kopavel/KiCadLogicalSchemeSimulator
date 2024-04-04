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
package lv.pko.DigitalNetSimulator.components.oscillator;
import lv.pko.DigitalNetSimulator.tools.Log;
import lv.pko.DigitalNetSimulator.tools.UiTools;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OscillatorUi extends JFrame {
    private final OscillatorUiComponent parent;
    public JTextField periodTextField;
    public JTextField freqTextField;
    ScheduledExecutorService scheduler;
    DecimalFormat df = new DecimalFormat("#,###");
    private JButton startButton;
    private JButton stopButton;
    private JPanel panel;
    private JButton oneTickButton;
    private JTextField tickAmount;
    private JButton doTicks;
    private JTextField totalTicks;
    private long lastTicks;
    private double freq = 0;

    public OscillatorUi(OscillatorUiComponent parent) {
        this.parent = parent;
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setContentPane(panel);
        setLocationRelativeTo(parent); // Center the frame
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
        periodTextField.getDocument().addDocumentListener(new UiTools.TextChangeListener() {
            @Override
            protected void textChanged() {
                String text = periodTextField.getText();
                if (!text.isBlank()) {
                    parent.parent.setClockPeriod(Long.parseLong(periodTextField.getText()));
                }
            }
        });
        oneTickButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    parent.parent.tick();
                } catch (Exception ex) {
                    Log.error(OscillatorUi.class, "Error in tick", ex);
                }
//                stateButton.setText(parent.parent.state ? "1" : "0");
            }
        });
        doTicks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
        pack();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            SwingUtilities.invokeLater(() -> periodTextField.setText(String.valueOf(parent.parent.getClockPeriod())));
        }
    }

    private void tick() {
        double newFreq = parent.parent.ticks - lastTicks;
        if (freq < newFreq * 0.9 || freq > newFreq * 1.1) {
            freq = newFreq;
        } else {
            freq = (freq * 0.8) + ((parent.parent.ticks - lastTicks) * 0.2);
        }
        lastTicks = parent.parent.ticks;
        SwingUtilities.invokeLater(() -> {
            freqTextField.setText(df.format(freq / 2));
            totalTicks.setText(String.valueOf(parent.parent.ticks));
        });
    }
}
