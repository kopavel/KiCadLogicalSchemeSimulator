/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.ui.main;
import pko.KiCadLogicalSchemeSimulator.Simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainUI extends JFrame {
    public MainUI() {
        JPanel var1 = new JPanel();
        var1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        var1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0), null, 0, 0, null, null));
        setContentPane(var1);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Simulator.net != null && !Simulator.net.stabilizing) {
                    Simulator.saveLayout();
                }
                System.exit(0);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                if (e.getOppositeWindow() == null) {
                    Simulator.bringToFront();
                }
            }
        });
        setSize(500, 400);
        setLocationRelativeTo(null); // Center the frame
    }
}
