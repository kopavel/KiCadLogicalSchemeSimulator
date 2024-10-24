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
package pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.ui.main.MainMenu.mainI81n;

public class OscilloscopeMenu extends JMenuBar {
    private final Oscilloscope oscilloscope;

    public OscilloscopeMenu(Oscilloscope parent) {
        oscilloscope = parent;
        JMenu pins = new JMenu(mainI81n.getString("schemaParts"));
        add(pins);
        fillPinsMenu(pins);
        JMenu presets = new JMenu(Oscilloscope.localization.getString("preset"));
        add(presets);
        JMenuItem reset = new JMenuItem(Oscilloscope.localization.getString("reset"));
        presets.add(reset);
        reset.addActionListener(e -> {
            parent.reset(true);
        });
        JMenuItem load = new JMenuItem(Oscilloscope.localization.getString("load"));
        presets.add(load);
        load.addActionListener(e -> {
            loadPreset(parent);
        });
        JMenuItem save = new JMenuItem(Oscilloscope.localization.getString("saveAs"));
        presets.add(save);
        save.addActionListener(e -> {
            savePreset(parent);
        });
    }

    private void fillPinsMenu(JMenu schemaParts) {
        Map<Character, JMenu> letterMenus = new HashMap<>();
        for (SchemaPart schemaPart : Simulator.net.schemaParts.values()) {
            char firstLetter = Character.toUpperCase(schemaPart.id.charAt(0));
            letterMenus.putIfAbsent(firstLetter, new JMenu(String.valueOf(firstLetter)));
            JMenu schemaPartItem = new JMenu(schemaPart.id);
            letterMenus.get(firstLetter).add(schemaPartItem);
            schemaPartItem.add(new JLabel(Oscilloscope.localization.getString("inPins")));
            schemaPart.inPins.values()
                    .stream().distinct().sorted().forEach(inItem -> {
                          JMenuItem inPinItem = new JMenuItem(schemaPart.ids.get(inItem));
                          inPinItem.addActionListener(e -> oscilloscope.addPin(inItem, schemaPart.ids.get(inItem), false));
                          schemaPartItem.add(inPinItem);
                      });
            schemaPartItem.addSeparator();
            schemaPartItem.add(new JLabel(Oscilloscope.localization.getString("outPins")));
            schemaPart.outPins.values()
                    .stream().distinct().sorted().forEach(inItem -> {
                          JMenuItem inPinItem = new JMenuItem(schemaPart.ids.get(inItem));
                          inPinItem.addActionListener(e -> oscilloscope.addPin(inItem, schemaPart.ids.get(inItem), true));
                          schemaPartItem.add(inPinItem);
                      });
        }
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            if (letterMenus.containsKey(letter)) {
                schemaParts.add(letterMenus.get(letter));
            }
        }
    }

    private void savePreset(Oscilloscope parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        fileChooser.setDialogTitle(Oscilloscope.localization.getString("saveAs"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Oscilloscope.localization.getString("presetFile"), "sym_oscill");
        fileChooser.setFileFilter(filter);
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            FileFilter selectedFilter = fileChooser.getFileFilter();
            if (selectedFilter instanceof FileNameExtensionFilter extensionFilter) {
                String extension = "." + extensionFilter.getExtensions()[0];
                if (!filePath.endsWith(extension)) {
                    filePath = filePath + extension;
                }
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                for (Diagram.PinItem pin : parent.diagram.pins) {
                    bw.write(pin.pin.parent.id + ':' + ((pin.out) ? 'O' : 'I') + ':' + pin.pin.id + '\n');
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void loadPreset(Oscilloscope parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        fileChooser.setDialogTitle(Oscilloscope.localization.getString("load"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Oscilloscope.localization.getString("presetFile"), "sym_oscill");
        fileChooser.setFileFilter(filter);
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            FileFilter selectedFilter = fileChooser.getFileFilter();
            if (selectedFilter instanceof FileNameExtensionFilter extensionFilter) {
                String extension = "." + extensionFilter.getExtensions()[0];
                if (!filePath.endsWith(extension)) {
                    filePath = filePath + extension;
                }
            }
            parent.reset(false);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
                String content;
                while ((content = reader.readLine()) != null) {
                    String[] split = content.split(":");
                    SchemaPart schemaPart = Simulator.net.schemaParts.get(split[0]);
                    ModelItem<?> inItem;
                    if ("I".equals(split[1])) {
                        inItem = schemaPart.inPins.get(split[2]);
                        oscilloscope.addPin(inItem, schemaPart.ids.get(inItem), false);
                    } else {
                        inItem = schemaPart.outPins.get(split[2]);
                        oscilloscope.addPin(inItem, schemaPart.ids.get(inItem), true);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
