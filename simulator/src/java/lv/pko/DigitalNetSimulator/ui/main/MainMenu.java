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
package lv.pko.DigitalNetSimulator.ui.main;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import lv.pko.DigitalNetSimulator.Simulator;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.tools.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainMenu extends JMenuBar {
    private static final Logger log = LoggerFactory.getLogger(MainMenu.class);
    private static final ResourceBundle mainI81n = ResourceBundle.getBundle("i81n/main");

    public MainMenu() {
        JMenu fileMenu = new JMenu(mainI81n.getString("file"));
        add(fileMenu);
        JMenuItem exitItem = new JMenuItem(mainI81n.getString("exit"));
        exitItem.addActionListener(e -> {
            Simulator.saveLayout();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        JMenuItem saveItem = new JMenuItem(mainI81n.getString("saveLayout"));
        saveItem.addActionListener(e -> Simulator.saveLayout());
        fileMenu.add(saveItem);
        JMenuItem loadItem = new JMenuItem(mainI81n.getString("loadLayout"));
        loadItem.addActionListener(e -> Simulator.loadLayout());
        fileMenu.add(loadItem);
        JMenu simulationMenu = new JMenu(mainI81n.getString("simulation"));
        add(simulationMenu);
        JMenuItem oscilloscopeItem = new JMenuItem(mainI81n.getString("showOscilloscope"));
        oscilloscopeItem.addActionListener(e -> Simulator.oscilloscope.bringUp());
        simulationMenu.add(oscilloscopeItem);
        JMenu schemaParts = new JMenu(mainI81n.getString("schemaParts"));
        for (Chip schemaPart : Simulator.model.chips.values()) {
            JMenuItem schemaPartItem = new JMenuItem(schemaPart.id);
            schemaPartItem.addActionListener(e -> Simulator.addMonitoringPart(schemaPart.id, null));
            schemaParts.add(schemaPartItem);
        }
        simulationMenu.add(schemaParts);
        JMenu settings = new JMenu(mainI81n.getString("settings"));
        add(settings);
        JMenu lang = new JMenu(mainI81n.getString("langs"));
        settings.add(lang);
        JMenuItem engLang = new JMenuItem(mainI81n.getString("engLang"));
        lang.add(engLang);
        JMenuItem lvLang = new JMenuItem(mainI81n.getString("lvLang"));
        lang.add(lvLang);
        JMenuItem ruLang = new JMenuItem(mainI81n.getString("ruLang"));
        lang.add(ruLang);
        engLang.addActionListener(e -> {
            setLocale("en");
        });
        lvLang.addActionListener(e -> {
            setLocale("lv");
        });
        ruLang.addActionListener(e -> {
            setLocale("ru");
        });
        JMenu themeMenu = new JMenu(mainI81n.getString("themes"));
        settings.add(themeMenu);
        JMenuItem lightTheme = new JMenuItem(mainI81n.getString("light"));
        lightTheme.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
                FlatLaf.updateUI();
            } catch (UnsupportedLookAndFeelException ex) {
                Log.error(MainMenu.class, "LookAndFill error", ex);
            }
        });
        themeMenu.add(lightTheme);
        JMenuItem darkTheme = new JMenuItem(mainI81n.getString("dark"));
        darkTheme.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                FlatLaf.updateUI();
            } catch (UnsupportedLookAndFeelException ex) {
                Log.error(MainMenu.class, "LookAndFill error", ex);
            }
        });
        themeMenu.add(darkTheme);
    }

    private static void setLocale(String ru) {
        Locale.setDefault(Locale.of(ru));
        Simulator.saveLayout();
        JOptionPane.showMessageDialog(null, ResourceBundle.getBundle("i81n/main").getString("doRestart"), "", JOptionPane.INFORMATION_MESSAGE);
    }
}
