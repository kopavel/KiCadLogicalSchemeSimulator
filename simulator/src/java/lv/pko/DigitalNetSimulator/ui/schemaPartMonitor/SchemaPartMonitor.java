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
package lv.pko.DigitalNetSimulator.ui.schemaPartMonitor;
import lv.pko.DigitalNetSimulator.Simulator;
import lv.pko.DigitalNetSimulator.api.AbstractUiComponent;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;
import lv.pko.DigitalNetSimulator.api.pins.out.TriStateOutPin;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lv.pko.DigitalNetSimulator.tools.Utils.addToArray;
import static org.apache.commons.lang3.StringUtils.leftPad;

public class SchemaPartMonitor extends JFrame {
    public final Chip schemaPart;
    private final ScheduledExecutorService scheduler;
    private final JTextArea extraPanel;
    InPin[] ins = new InPin[0];
    OutPin[] outs = new OutPin[0];
    JLabel[] insLabels = new JLabel[0];
    JLabel[] outsLabels = new JLabel[0];
    Long[] insMask = new Long[0];
    Long[] outsMask = new Long[0];
    private JPanel inputsNames;
    private JPanel inputsValues;
    private JPanel outputsNames;
    private JPanel outputsValues;
    private JLabel title;
    private JPanel panel;
    private JPanel chipBox;

    public SchemaPartMonitor(String id) {
        Color borderColor = UIManager.getColor("TextField.border");
        setContentPane(panel);
        setLocationRelativeTo(Simulator.ui);
        setType(Window.Type.UTILITY);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
                Simulator.removeMonitoringPart(schemaPart.id);
                dispose();
            }
        });
        schemaPart = Simulator.model.chips.get(id);
        title.setText(id);
        chipBox.setBorder(BorderFactory.createLineBorder(borderColor));
        for (InPin inPin : schemaPart.inMap.values()) {
            JLabel label;
            if (inPin.useBitPresentation) {
                char[] bits = leftPad(Long.toBinaryString(inPin.getState()), inPin.size, '0').toCharArray();
                for (int j = 0; j < inPin.size; j++) {
                    label = new JLabel(inPin.id + j);
                    label.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
                    label.setAlignmentX(Component.LEFT_ALIGNMENT);
                    label.setFont(AbstractUiComponent.monospacedFont);
                    inputsNames.add(label);
                    label = new JLabel(String.valueOf(bits[j]));
                    label.setFont(AbstractUiComponent.monospacedFont);
                    label.setAlignmentX(RIGHT_ALIGNMENT);
                    label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 0),
                            new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                    ins = addToArray(ins, inPin);
                    insLabels = addToArray(insLabels, label);
                    inputsValues.add(label);
                    insMask = addToArray(insMask, 1L << j);
                }
            } else {
                label = new JLabel(inPin.id);
                label.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                label.setFont(AbstractUiComponent.monospacedFont);
                inputsNames.add(label);
                label = new JLabel(String.format("%" + (int) Math.ceil(inPin.size / 4d) + "X", inPin.getState()));
                label.setFont(AbstractUiComponent.monospacedFont);
                label.setAlignmentX(RIGHT_ALIGNMENT);
                label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 0),
                        new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                ins = addToArray(ins, inPin);
                insLabels = addToArray(insLabels, label);
                inputsValues.add(label);
            }
        }
        for (OutPin pin : schemaPart.outMap.values()) {
            JLabel label;
            if (pin.useBitPresentation) {
                char[] bits = leftPad(Long.toBinaryString(pin.state), pin.size, '0').toCharArray();
                for (int j = 0; j < pin.size; j++) {
                    label = new JLabel(pin.id + j);
                    label.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 2));
                    label.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    label.setFont(AbstractUiComponent.monospacedFont);
                    outputsNames.add(label);
                    label = new JLabel(String.valueOf(bits[j]));
                    label.setFont(AbstractUiComponent.monospacedFont);
                    label.setAlignmentX(LEFT_ALIGNMENT);
                    label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 0, 1, 2),
                            new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                    outputsValues.add(label);
                    outs = addToArray(outs, pin);
                    outsLabels = addToArray(outsLabels, label);
                    outsMask = addToArray(outsMask, 1L << j);
                }
            } else {
                label = new JLabel(pin.id);
                label.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 2));
                label.setAlignmentX(Component.RIGHT_ALIGNMENT);
                label.setFont(AbstractUiComponent.monospacedFont);
                outputsNames.add(label);
                label = new JLabel(String.format("%" + (int) Math.ceil(pin.size / 4d) + "X", pin.state));
                label.setFont(AbstractUiComponent.monospacedFont);
                label.setAlignmentX(LEFT_ALIGNMENT);
                label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 0, 1, 2),
                        new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                outputsValues.add(label);
                outs = addToArray(outs, pin);
                outsLabels = addToArray(outsLabels, label);
            }
        }
        extraPanel = new JTextArea();
        extraPanel.setFont(AbstractUiComponent.monospacedFont);
        extraPanel.setEditable(false);
        if (schemaPart.outMap.size() > schemaPart.inMap.size()) {
            inputsNames.add(extraPanel);
            extraPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else {
            outputsNames.add(extraPanel);
            extraPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        }
        extraPanel.setFont(AbstractUiComponent.monospacedFont);
        if (schemaPart.extraState() != null) {
            extraPanel.setVisible(true);
            extraPanel.setText(schemaPart.extraState());
            extraPanel.revalidate();
            extraPanel.repaint();
        } else {
            extraPanel.setVisible(false);
        }
        //setSize(new Dimension(250, 90 + Math.max(schemaPart.inMap.size(), schemaPart.outMap.size()) * 16));
        pack();
//        setSize(new Dimension(200, 78));
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reDraw, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void reDraw() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < ins.length; i++) {
                if (ins[i].useBitPresentation) {
                    insLabels[i].setText((ins[i].getState() & insMask[i]) > 0 ? "1" : "0");
                } else {
                    insLabels[i].setText(String.format("%" + (int) Math.ceil(ins[i].size / 4d) + "X", ins[i].getState()));
                }
            }
            for (int i = 0; i < outs.length; i++) {
                OutPin out = outs[i];
                if (out instanceof TriStateOutPin triStateOutPin && triStateOutPin.hiImpedance) {
                    outsLabels[i].setText("Hi");
                } else if (out.useBitPresentation) {
                    outsLabels[i].setText((out.state & outsMask[i]) > 0 ? "1" : "0");
                } else {
                    outsLabels[i].setText(String.format("%" + (int) Math.ceil(out.size / 4d) + "X", out.state));
                }
            }
            String extraState = schemaPart.extraState();
            if (extraState != null) {
                extraPanel.setVisible(true);
                extraPanel.setText(extraState);
            } else {
                extraPanel.setVisible(false);
            }
        });
    }

    private void createUIComponents() {
        inputsNames = new JPanel();
        inputsNames.setLayout(new BoxLayout(inputsNames, BoxLayout.Y_AXIS));
        outputsNames = new JPanel();
        outputsNames.setLayout(new BoxLayout(outputsNames, BoxLayout.Y_AXIS));
        inputsValues = new JPanel();
        inputsValues.setLayout(new BoxLayout(inputsValues, BoxLayout.Y_AXIS));
        outputsValues = new JPanel();
        outputsValues.setLayout(new BoxLayout(outputsValues, BoxLayout.Y_AXIS));
    }
}
