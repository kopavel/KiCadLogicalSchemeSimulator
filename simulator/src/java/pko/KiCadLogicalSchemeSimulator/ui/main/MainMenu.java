/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.ui.main;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import javax.swing.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MainMenu extends JMenuBar {
    public static final ResourceBundle mainI81n = ResourceBundle.getBundle("i81n/main");

    public MainMenu() {
        JMenu schemaParts = new JMenu(mainI81n.getString("schemaParts"));
        Map<Character, JMenu> letterMenus = new HashMap<>();
        add(schemaParts);
        for (SchemaPart schemaPart : Simulator.net.schemaParts.values()) {
            if (!"Power".equals(schemaPart.getClass().getSimpleName())) {
                char firstLetter = Character.toUpperCase(schemaPart.id.charAt(0));
                letterMenus.putIfAbsent(firstLetter, new JMenu(String.valueOf(firstLetter)));
                JMenuItem schemaPartItem = new JMenuItem(schemaPart.id);
                schemaPartItem.addActionListener(e -> Simulator.addMonitoringPart(schemaPart.id, null));
                letterMenus.get(firstLetter).add(schemaPartItem);
            }
        }
        for (char letter = '0'; letter <= 'Z'; letter++) {
            if (letterMenus.containsKey(letter)) {
                schemaParts.add(letterMenus.get(letter));
            }
        }
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
        engLang.addActionListener(e -> setLocale("en"));
        lvLang.addActionListener(e -> setLocale("lv"));
        ruLang.addActionListener(e -> setLocale("ru"));
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
