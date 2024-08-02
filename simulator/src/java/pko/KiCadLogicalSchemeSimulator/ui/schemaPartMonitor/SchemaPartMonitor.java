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
package pko.KiCadLogicalSchemeSimulator.ui.schemaPartMonitor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.leftPad;

//FixMe add passive pins menu
public class SchemaPartMonitor extends JFrame {
    public final SchemaPart schemaPart;
    private final ScheduledExecutorService scheduler;
    private final JTextArea extraPanel;
    Item[] ins = new Item[0];
    Item[] outs = new Item[0];
    private JPanel inputsNames;
    private JPanel inputsValues;
    private JPanel outputsNames;
    private JPanel outputsValues;
    private JLabel title;
    private JPanel panel;
    private JPanel schemaPartBox;

    public SchemaPartMonitor(String id) {
        setupUI();
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
        schemaPart = Simulator.model.schemaParts.get(id);
        title.setText(id);
        schemaPartBox.setBorder(BorderFactory.createLineBorder(borderColor));
        schemaPart.inPins.values()
                .stream().distinct().sorted().forEach(inItem -> {
                      JLabel label;
                      if (inItem instanceof Bus bus && bus.useBitPresentation) {
                          char[] bits = leftPad(Long.toBinaryString(bus.state), bus.size, '0').toCharArray();
                          for (int j = 0; j < bus.size; j++) {
                              label = new JLabel(bus.id + j);
                              label.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
                              label.setAlignmentX(Component.LEFT_ALIGNMENT);
                              label.setFont(AbstractUiComponent.monospacedFont);
                              inputsNames.add(label);
                              label = new JLabel(String.valueOf(bits[j]));
                              label.setFont(AbstractUiComponent.monospacedFont);
                              label.setAlignmentX(RIGHT_ALIGNMENT);
                              label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 0),
                                      new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                              ins = Utils.addToArray(ins, new Item(inItem, label, 1L << j));
                              inputsValues.add(label);
                          }
                      } else {
                          label = new JLabel(inItem.getId());
                          label.setBorder(BorderFactory.createEmptyBorder(3, 2, 4, 0));
                          label.setAlignmentX(Component.LEFT_ALIGNMENT);
                          label.setFont(AbstractUiComponent.monospacedFont);
                          inputsNames.add(label);
                          label = new JLabel(String.format("%" + (int) Math.ceil(inItem.getSize() / 4d) + "X", inItem.getState()));
                          label.setFont(AbstractUiComponent.monospacedFont);
                          label.setAlignmentX(RIGHT_ALIGNMENT);
                          label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 0),
                                  new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                          ins = Utils.addToArray(ins, new Item(inItem, label, 1L));
                          inputsValues.add(label);
                      }
                  });
        schemaPart.outPins.values()
                .stream().distinct().forEach(outItem -> {
                      JLabel label;
                      if (outItem instanceof Bus bus && bus.useBitPresentation) {
                          char[] bits = leftPad(Long.toBinaryString(bus.state), bus.size, '0').toCharArray();
                          for (int j = 0; j < bus.size; j++) {
                              label = new JLabel(bus.id + j);
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
                              outs = Utils.addToArray(outs, new Item(outItem, label, 1L << j));
                          }
                      } else {
                          label = new JLabel(outItem.getId());
                          label.setBorder(BorderFactory.createEmptyBorder(3, 0, 4, 2));
                          label.setAlignmentX(Component.RIGHT_ALIGNMENT);
                          label.setFont(AbstractUiComponent.monospacedFont);
                          outputsNames.add(label);
                          label = new JLabel(String.format("%" + (int) Math.ceil(outItem.getSize() / 4d) + "X", outItem.getState()));
                          label.setFont(AbstractUiComponent.monospacedFont);
                          label.setAlignmentX(LEFT_ALIGNMENT);
                          label.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(2, 0, 1, 2),
                                  new CompoundBorder(BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(1, 5, 1, 5))));
                          outputsValues.add(label);
                          outs = Utils.addToArray(outs, new Item(outItem, label, 1L));
                      }
                  });
        extraPanel = new JTextArea();
        extraPanel.setFont(AbstractUiComponent.monospacedFont);
        extraPanel.setEditable(false);
        if (schemaPart.outPins.size() > schemaPart.inPins.size()) {
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
            for (Item in : ins) {
                if (in.item.isHiImpedance()) {
                    in.label.setText("Hi");
                } else if (in.item instanceof Bus bus && bus.useBitPresentation) {
                    in.label.setText((bus.state & in.mask) > 0 ? "1" : "0");
                } else {
                    in.label.setText(String.format("%" + (int) Math.ceil(in.item.getSize() / 4d) + "X", in.item.getState()));
                }
            }
            for (Item out : outs) {
                if (out.item.isHiImpedance()) {
                    out.label.setText("Hi");
                } else if (out.item instanceof Bus bus && bus.useBitPresentation) {
                    out.label.setText((bus.state & out.mask) > 0 ? "1" : "0");
                } else {
                    out.label.setText(String.format("%" + (int) Math.ceil(out.item.getSize() / 4d) + "X", out.item.getState()));
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

    private void setupUI() {
        this.createUIComponents();
        JPanel var1 = new JPanel();
        this.panel = var1;
        var1.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), 0, 0, false, false));
        JPanel var2 = new JPanel();
        this.schemaPartBox = var2;
        var2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 0, 0, false, false));
        var1.add(var2, new GridConstraints(1, 1, 1, 1, 0, 3, 7, 3, null, null, null));
        JPanel var3 = this.inputsNames;
        var2.add(var3, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        JPanel var4 = this.outputsNames;
        var2.add(var4, new GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        JLabel var5 = new JLabel();
        this.title = var5;
        var5.setText("Label");
        var1.add(var5, new GridConstraints(0, 1, 1, 1, 1, 0, 0, 0, null, null, null));
        JPanel var6 = this.inputsValues;
        var1.add(var6, new GridConstraints(1, 0, 1, 1, 4, 2, 3, 3, null, null, null));
        JPanel var7 = this.outputsValues;
        var1.add(var7, new GridConstraints(1, 2, 1, 1, 8, 2, 3, 3, null, null, null));
    }

    private record Item(IModelItem item, JLabel label, Long mask) {
    }
}
