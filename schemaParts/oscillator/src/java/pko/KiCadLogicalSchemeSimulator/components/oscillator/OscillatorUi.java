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
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope.Oscilloscope;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
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
        $$$setupUI$$$();
        this.parent = parent;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
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
                    try {
                        int amount = Integer.parseInt(tickAmount.getText());
                        for (int i = 0; i < amount; i++) {
                            parent.parent.tick();
                        }
                    } catch (Exception ex) {
                        Log.error(OscillatorUi.class, "Error in tick", ex);
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

    private void $$$setupUI$$$() {
        setResizable(false);
        JPanel var1 = new JPanel();
        panel = var1;
        var1.setLayout(new GridLayoutManager(7, 3, new Insets(10, 10, 10, 10), -1, -1, false, false));
        var1.setOpaque(true);
        var1.setPreferredSize(new Dimension(500, 200));
        JTextField var2 = new JTextField();
        freqTextField = var2;
        var2.setText("");
        var1.add(var2, new GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(40, 30), null));
        JLabel var3 = new JLabel();
        var3.setHorizontalAlignment(4);
        $$$loadLabelText$$$(var3, ResourceBundle.getBundle("i81n_clock/clock").getString("freq"));
        var1.add(var3, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, new Dimension(80, -1), null));
        JLabel var4 = new JLabel();
        var4.setHorizontalAlignment(4);
        $$$loadLabelText$$$(var4, ResourceBundle.getBundle("i81n_clock/clock").getString("resultFreq"));
        var1.add(var4, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, new Dimension(80, -1), null));
        JTextField var5 = new JTextField();
        achievedTextField = var5;
        var5.setEditable(false);
        var5.setEnabled(true);
        var1.add(var5, new GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(40, 30), null));
        JButton var6 = new JButton();
        oneTickButton = var6;
        var6.setText(" +1");
        var1.add(var6, new GridConstraints(5, 0, 1, 1, 0, 1, 3, 0, null, new Dimension(20, 30), null, 1));
        JLabel var7 = new JLabel();
        var7.setHorizontalAlignment(4);
        var7.setHorizontalTextPosition(0);
        $$$loadLabelText$$$(var7, ResourceBundle.getBundle("i81n_clock/clock").getString("manualTick"));
        var1.add(var7, new GridConstraints(4, 0, 1, 1, 0, 0, 0, 1, null, null, null));
        JButton var8 = new JButton();
        stopButton = var8;
        $$$loadButtonText$$$(var8, ResourceBundle.getBundle("i81n_clock/clock").getString("stopClock"));
        var1.add(var8, new GridConstraints(1, 2, 1, 1, 2, 1, 3, 0, null, new Dimension(60, 30), null));
        JButton var9 = new JButton();
        startButton = var9;
        $$$loadButtonText$$$(var9, ResourceBundle.getBundle("i81n_clock/clock").getString("startClock"));
        var1.add(var9, new GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, new Dimension(60, 30), null));
        JTextField var10 = new JTextField();
        tickAmount = var10;
        var1.add(var10, new GridConstraints(5, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(40, 30), null));
        JButton var11 = new JButton();
        doTicks = var11;
        var11.setText(">>");
        var1.add(var11, new GridConstraints(5, 2, 1, 1, 0, 1, 3, 0, null, new Dimension(20, 30), null));
        JSeparator var12 = new JSeparator();
        var1.add(var12, new GridConstraints(3, 0, 1, 3, 1, 1, 0, 2, null, new Dimension(-1, 5), null));
        JLabel var13 = new JLabel();
        var13.setHorizontalAlignment(4);
        $$$loadLabelText$$$(var13, ResourceBundle.getBundle("i81n_clock/clock").getString("totalTick"));
        var1.add(var13, new GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, null, new Dimension(80, -1), null));
        JTextField var14 = new JTextField();
        totalTicks = var14;
        var14.setEditable(false);
        var1.add(var14, new GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, 30), null));
        JLabel var15 = new JLabel();
        $$$loadLabelText$$$(var15, ResourceBundle.getBundle("i81n_clock/clock").getString("skip"));
        var1.add(var15, new GridConstraints(4, 1, 1, 1, 8, 0, 0, 0, null, null, null, 1));
        Spacer var16 = new Spacer();
        var1.add(var16, new GridConstraints(6, 1, 1, 1, 0, 2, 1, 6, null, null, null));
        JButton var17 = new JButton();
        oscilloscopeButton = var17;
        $$$loadButtonText$$$(var17, ResourceBundle.getBundle("i81n_clock/clock").getString("showOscilloscope"));
        var1.add(var17, new GridConstraints(2, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        var3.setLabelFor(var2);
        var4.setLabelFor(var5);
    }

    private void $$$loadLabelText$$$(JLabel var1, String var2) {
        StringBuilder var3 = new StringBuilder();
        boolean var4 = false;
        char var5 = 0;
        int var6 = -1;
        for (int var7 = 0; var7 < var2.length(); ++var7) {
            if (var2.charAt(var7) == '&') {
                ++var7;
                if (var7 == var2.length()) {
                    break;
                }
                if (!var4 && var2.charAt(var7) != '&') {
                    var4 = true;
                    var5 = var2.charAt(var7);
                    var6 = var3.length();
                }
            }
            var3.append(var2.charAt(var7));
        }
        var1.setText(var3.toString());
        if (var4) {
            var1.setDisplayedMnemonic(var5);
            var1.setDisplayedMnemonicIndex(var6);
        }
    }

    // $FF: synthetic method
    private void $$$loadButtonText$$$(AbstractButton var1, String var2) {
        StringBuilder var3 = new StringBuilder();
        boolean var4 = false;
        char var5 = 0;
        int var6 = -1;
        for (int var7 = 0; var7 < var2.length(); ++var7) {
            if (var2.charAt(var7) == '&') {
                ++var7;
                if (var7 == var2.length()) {
                    break;
                }
                if (!var4 && var2.charAt(var7) != '&') {
                    var4 = true;
                    var5 = var2.charAt(var7);
                    var6 = var3.length();
                }
            }
            var3.append(var2.charAt(var7));
        }
        var1.setText(var3.toString());
        if (var4) {
            var1.setMnemonic(var5);
            var1.setDisplayedMnemonicIndex(var6);
        }
    }
}
