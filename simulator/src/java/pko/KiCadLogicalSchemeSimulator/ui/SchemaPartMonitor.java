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
package pko.KiCadLogicalSchemeSimulator.ui;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.UiTools;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static javax.swing.SwingConstants.LEADING;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.TRAILING;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static pko.KiCadLogicalSchemeSimulator.tools.Utils.AlphanumericComparator;

public class SchemaPartMonitor extends JFrame {
    public static final Font FONT = AbstractUiComponent.monospacedFont;
    public final SchemaPart schemaPart;
    private final ScheduledExecutorService scheduler;
    private final JTextArea extraPanel;
    private final double[] fontSize;
    Item[] ins = new Item[0];
    Item[] outs = new Item[0];
    private JPanel inputsNames;
    private JPanel inputsValues;
    private JPanel outputsNames;
    private JPanel outputsValues;
    private JPanel panel;
    private JPanel schemaPartBox;

    public SchemaPartMonitor(String id) {
        //region init
        setupUI();
        Color borderColor = UIManager.getColor("TextField.border");
        setContentPane(panel);
        setLocationRelativeTo(Simulator.ui);
        setType(Window.Type.UTILITY);
        setTitle(id);
        fontSize = UiTools.getFontSize(FONT);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
                Simulator.removeMonitoringPart(schemaPart.id);
                dispose();
            }
        });
        schemaPart = Simulator.net.schemaParts.get(id);
        schemaPartBox.setBorder(BorderFactory.createLineBorder(borderColor));
        //endregion
        //region In pins
        //FixMe passive pin like IN?
        ins = getItems(inputsNames,
                inputsValues,
                schemaPart.inPins.values()
                        .stream().distinct().sorted(AlphanumericComparator.comparing(ModelItem::getId)).toList(),
                borderColor,
                false);
        //endregion
        //region Out pins
        outs = getItems(outputsNames,
                outputsValues,
                schemaPart.outPins.values()
                        .stream().distinct().sorted(AlphanumericComparator.comparing(ModelItem::getId)).toList(),
                borderColor,
                true);
        //endregion
        //region info panel
        extraPanel = new JTextArea();
        extraPanel.setFont(FONT);
        extraPanel.setEditable(false);
        if (schemaPart.outPins.size() > schemaPart.inPins.size()) {
            inputsNames.add(extraPanel);
            extraPanel.setAlignmentX(LEFT_ALIGNMENT);
        } else {
            outputsNames.add(extraPanel);
            extraPanel.setAlignmentX(RIGHT_ALIGNMENT);
        }
        extraPanel.setFont(FONT);
        if (schemaPart.extraState() != null) {
            extraPanel.setVisible(true);
            extraPanel.setText(schemaPart.extraState());
            extraPanel.revalidate();
            extraPanel.repaint();
        } else {
            extraPanel.setVisible(false);
        }
        //endregion
        //region Extra Panel Setup
        Supplier<JPanel> extraPannelSupplier = schemaPart.extraPanel();
        if (extraPannelSupplier != null) {
            JButton extraPanelButton = new JButton();
            extraPanelButton.setText(">");
            extraPanelButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        JFrame jFrame = new JFrame();
                        jFrame.setTitle(schemaPart.id + ":extra");
                        JPanel extraPanel = extraPannelSupplier.get();
                        jFrame.getContentPane().add(extraPanel, BorderLayout.LINE_START);
                        jFrame.pack();
                        jFrame.setLocationRelativeTo(null);
                        jFrame.setVisible(true);
                        extraPanel.setVisible(true);
                    });
                }
            });
            if (schemaPart.outPins.size() > schemaPart.inPins.size()) {
                inputsNames.add(extraPanelButton);
                extraPanelButton.setAlignmentX(LEFT_ALIGNMENT);
            } else {
                outputsNames.add(extraPanelButton);
                extraPanelButton.setAlignmentX(RIGHT_ALIGNMENT);
            }
        }
        //endregion
        pack();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reDraw, 0, 100, TimeUnit.MILLISECONDS);
    }

    private Item[] getItems(JPanel names, JPanel values, Iterable<ModelItem<?>> items, Color borderColor, boolean right) {
        List<Item> retVal = new ArrayList<>();
        for (ModelItem<?> item : items) {
            if (item instanceof Bus bus) {
                if (bus.useBitPresentation) {
                    for (int j = 0; j < bus.size; j++) {
                        char[] bits = leftPad(Integer.toBinaryString(bus.state), bus.size, '0').toCharArray();
                        retVal.add(new Item(item, getLabel(schemaPart.ids.get(bus) + j, String.valueOf(bits[j]), names, values, borderColor, right), 1 << j));
                    }
                } else {
                    retVal.add(new Item(item,
                            getLabel(schemaPart.ids.get(bus), leftPad("", (int) Math.ceil(bus.size / 4.0), '0'), names, values, borderColor, right),
                            1));
                }
            } else {
                retVal.add(new Item(item, getLabel(schemaPart.ids.get(item), String.valueOf(item.getState()), names, values, borderColor, right), 1));
            }
        }
        return retVal.toArray(new Item[0]);
    }

    private JLabel getLabel(String id, String value, JPanel namePan, JPanel valuePan, Color borderColor, boolean right) {
        //region name
        JLabel label = new JLabel(id);
        if (right) {
            label.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 2));
        } else {
            label.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
        }
        label.setAlignmentX(right ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
        label.setFont(FONT);
        namePan.add(label);
        //endregion
        //region value
        label = new JLabel(value);
        label.setFont(FONT);
        label.setAlignmentX(right ? LEFT_ALIGNMENT : RIGHT_ALIGNMENT);
        label.setHorizontalAlignment(right ? LEADING : TRAILING);
        Border border = new CompoundBorder((right ? BorderFactory.createEmptyBorder(2, 0, 1, 2) : BorderFactory.createEmptyBorder(2, 2, 1, 0)),
                new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5)));
        label.setBorder(border);
        Insets borderInsets = border.getBorderInsets(label);
        int maxTextWidth = (int) Math.ceil(fontSize[0] * Math.max(2, value.length()));
        label.setPreferredSize(new Dimension(maxTextWidth + borderInsets.left + borderInsets.right,
                (int) Math.ceil(fontSize[1] + borderInsets.top + borderInsets.bottom)));
        label.setMinimumSize(new Dimension(maxTextWidth + borderInsets.left + borderInsets.right,
                (int) Math.ceil(fontSize[1] + borderInsets.top + borderInsets.bottom)));
        label.setMaximumSize(new Dimension(maxTextWidth + borderInsets.left + borderInsets.right,
                (int) Math.ceil(fontSize[1] + borderInsets.top + borderInsets.bottom)));
        valuePan.add(label);
        //endregion
        return label;
    }

    private void reDraw() {
        SwingUtilities.invokeLater(() -> {
            for (Item in : ins) {
                if (in.item.isHiImpedance()) {
                    in.label.setText("Hi");
                } else if (in.item instanceof Bus bus && bus.useBitPresentation) {
                    in.label.setText((bus.state & in.mask) > 0 ? "1" : "0");
                } else {
                    in.label.setText(String.format("%0" + (int) Math.ceil(in.item.getSize() / 4.0d) + "X", in.item.getState()));
                }
            }
            for (Item out : outs) {
                if (out.item.isHiImpedance()) {
                    out.label.setText("Hi");
                } else if (out.item instanceof Bus bus && bus.useBitPresentation) {
                    out.label.setText((bus.getState() & out.mask) != 0 ? "1" : "0");
                } else {
                    out.label.setText(String.format("%0" + (int) Math.ceil(out.item.getSize() / 4.0d) + "X", out.item.getState()));
                }
                out.label.setHorizontalAlignment(LEFT);
            }
            String extraState = schemaPart.extraState();
            if (extraState != null) {
                extraPanel.setText(extraState);
            }
        });
    }

    private void setupUI() {
        panel = new JPanel(new GridBagLayout());
        schemaPartBox = new JPanel(new GridBagLayout());
        panel.add(schemaPartBox, createBagConstraints(1, 1, 1, 1));
        inputsNames = new JPanel();
        inputsNames.setLayout(new BoxLayout(inputsNames, BoxLayout.Y_AXIS));
        schemaPartBox.add(inputsNames, createBagConstraints(0, 0, 0.5, 1));
        outputsNames = new JPanel();
        outputsNames.setLayout(new BoxLayout(outputsNames, BoxLayout.Y_AXIS));
        schemaPartBox.add(outputsNames, createBagConstraints(1, 0, 0.5, 1));
        inputsValues = new JPanel();
        inputsValues.setLayout(new BoxLayout(inputsValues, BoxLayout.Y_AXIS));
        panel.add(inputsValues, createBagConstraints(0, 1, 0.3, 0));
        outputsValues = new JPanel();
        outputsValues.setLayout(new BoxLayout(outputsValues, BoxLayout.Y_AXIS));
        panel.add(outputsValues, createBagConstraints(2, 1, 0.3, 1));
    }

    private static GridBagConstraints createBagConstraints(int gridx, int gridY, double weightX, double weightY) {
        return new GridBagConstraints(gridx, gridY, 1, 1, weightX, weightY, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    }

    private record Item(IModelItem<?> item, JLabel label, Integer mask) {
    }
}
