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
package lv.pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import lv.pko.KiCadLogicalSchemeSimulator.components.oscillator.OscillatorUi;
import lv.pko.KiCadLogicalSchemeSimulator.v2.api.IModelItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//ToDo store/load oscilloscope presets...
//ToDo reset history
//ToDo search by pin changes
//ToDo navigate to start/end
//ToDo add 'time' tags
public class Oscilloscope extends JFrame {
    public static final ResourceBundle localization = ResourceBundle.getBundle("i81n_clock/clock");
    final Diagram diagram;
    private final JPanel watchedItemNamesPanel;
    private final ScheduledExecutorService scheduler;

    public Oscilloscope(OscillatorUi parent) {
        setJMenuBar(new OscilloscopeMenu(this));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scheduler.shutdown();
                parent.oscilloscope = null;
                parent.parent.parent.out = ((OscilloscopePin) parent.parent.parent.out).wrapped;
                parent.parent.parent.restartClock();
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
        parent.parent.parent.out = new OscilloscopePin(parent.parent.parent.out, this);
        parent.parent.parent.restartClock();
        setVisible(true);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reDraw, 0, 250, TimeUnit.MILLISECONDS);
        watchedItemNamesPanel.add(new FixedHeightLabel("clock"));
        watchedItemNamesPanel.revalidate();
        diagram.revalidate();
        diagram.addPin(parent.parent.parent.out, parent.parent.parent.out.getName());
    }

    public void addPin(IModelItem pin, String name) {
        watchedItemNamesPanel.add(new FixedHeightLabel(name));
        watchedItemNamesPanel.revalidate();
        //FixMe check if diagram need to be pinBased, not busBased
        diagram.addPin(pin, name);
    }

    public void reDraw() {
        SwingUtilities.invokeLater(() -> {
            diagram.revalidate();
            diagram.repaint();
        });
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
