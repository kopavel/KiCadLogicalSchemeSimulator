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
package lv.pko.DigitalNetSimulator;
import com.formdev.flatlaf.FlatIntelliJLaf;
import lv.pko.DigitalNetSimulator.api.AbstractUiComponent;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.chips.InteractiveChip;
import lv.pko.DigitalNetSimulator.model.Model;
import lv.pko.DigitalNetSimulator.parsers.net.NetFileParser;
import lv.pko.DigitalNetSimulator.parsers.pojo.Export;
import lv.pko.DigitalNetSimulator.parsers.xml.XmlParser;
import lv.pko.DigitalNetSimulator.tools.Log;
import lv.pko.DigitalNetSimulator.tools.Utils;
import lv.pko.DigitalNetSimulator.ui.main.MainMenu;
import lv.pko.DigitalNetSimulator.ui.main.MainUI;
import lv.pko.DigitalNetSimulator.ui.oscilloscope.Oscilloscope;
import lv.pko.DigitalNetSimulator.ui.schemaPartMonitor.SchemaPartMonitor;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@CommandLine.Command(name = "", description = "Start Kicad scheme interactive simulation")
public class Simulator implements Runnable {
    private static final Map<String, SchemaPartMonitor> monitoredParts = new HashMap<>();
    public static MainUI ui;
    public static String netFilePathNoExtension;
    public static Oscilloscope oscilloscope;
    public static Model model;
    @CommandLine.Parameters(index = "0", arity = "1", description = "Path to KiCad NET file")
    public String netFilePath;
    @CommandLine.Option(names = {"-m", "--mapFile"}, description = "Path to KiCad symbol mapping file")
    String mapFile;

    public static void main(String[] args) {
        new CommandLine(new Simulator()).execute(args);
    }

    public static void addMonitoringPart(String id, Rectangle sizes) {
        SwingUtilities.invokeLater(() -> {
            SchemaPartMonitor partMonitor = monitoredParts.computeIfAbsent(id, SchemaPartMonitor::new);
            partMonitor.setVisible(true);
//            partMonitor.toFront();
            if (sizes != null) {
                partMonitor.setBounds(sizes);
            }
        });
    }

    public static void bringToFront() {
        SwingUtilities.invokeLater(() -> monitoredParts.values().forEach(Window::toFront));
    }

    @SuppressWarnings("deprecation")
    public static void loadLayout() {
        //FixMe set all component size, if its not in layout file - it's buggy in swing if don't do that...
        try {
            File layoutFile = new File(netFilePathNoExtension + ".sym_layout");
            if (layoutFile.isFile()) {
                String content = Utils.readFileToString(layoutFile);
                for (String pos : content.split("[\r\n]+")) {
                    String[] parts = pos.split(":");
                    if (parts[0].equals("main")) {
                        ui.setBounds(new Rectangle(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
                    } else if (parts[0].equals("mon")) {
                        addMonitoringPart(parts[1],
                                new Rectangle(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])));
                    } else if (!parts[0].equals("locale")) {
                        Chip component = model.chips.get(parts[0]);
                        if (component instanceof InteractiveChip uiComponent) {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            AbstractUiComponent g = uiComponent.getComponent();
                            g.reshape(x, y, g.getWidth(), g.getHeight());
                            g.currentX = x;
                            g.currentY = y;
                            g.hasStoredLayout = true;
                        } else {
                            Log.warn(Simulator.class, "Unknown component {} in layout file", parts[0]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveLayout() throws RuntimeException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(netFilePathNoExtension + ".sym_layout"))) {
            bw.write("locale:" + Locale.getDefault().getLanguage());
            bw.write("\n");
            bw.write("main:" + ui.getX() + ":" + ui.getY() + ":" + ui.getWidth() + ":" + ui.getHeight());
            bw.write("\n");
            String content = model.chips.values()
                    .stream()
                    .filter(c -> c instanceof InteractiveChip)
                    .map(c -> c.id + ":" + ((InteractiveChip) c).getComponent().getX() + ":" + ((InteractiveChip) c).getComponent().getY())
                    .collect(Collectors.joining("\n"));
            bw.write(content);
            bw.write("\n");
            content = monitoredParts.values()
                    .stream()
                    .map(c -> "mon:" + c.schemaPart.id + ":" + c.getX() + ":" + c.getY() + ":" + c.getWidth() + ":" + c.getHeight())
                    .collect(Collectors.joining("\n"));
            bw.write(content);
            bw.write("\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeMonitoringPart(String id) {
        monitoredParts.remove(id);
    }

    @Override
    public void run() {
        try {
            if (!new File(netFilePath).exists()) {
                throw new Exception("Cant fine NET file " + netFilePath);
            }
            if (mapFile != null && !new File(mapFile).exists()) {
                throw new Exception("Can't fine Symbol map file " + mapFile);
            }
            netFilePathNoExtension = netFilePath.substring(0, netFilePath.lastIndexOf("."));
            loadLocale();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                } catch (UnsupportedLookAndFeelException e) {
                    throw new RuntimeException(e);
                }
                ui = new MainUI();
                ui.setVisible(true);
                oscilloscope = new Oscilloscope();
            });
            if (netFilePath.endsWith("xml")) {
                model = new Model(XmlParser.parse(netFilePath, Export.class), mapFile);
            } else if (netFilePath.endsWith(".net")) {
                model = new Model(new NetFileParser().parse(netFilePath), mapFile);
            } else {
                throw new RuntimeException("Unsupported file extension. " + netFilePath);
            }
            model.chips.values().forEach(chip -> {
                if (chip instanceof InteractiveChip) {
                    try {
                        SwingUtilities.invokeAndWait(() -> ui.add(((InteractiveChip) chip).getComponent()));
                    } catch (InterruptedException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                ui.setJMenuBar(new MainMenu());
                ui.revalidate();
                ui.repaint();
                for (Chip c : model.chips.values()) {
                    if (c instanceof InteractiveChip) {
                        AbstractUiComponent component = ((InteractiveChip) c).getComponent();
                        component.revalidate();
                        component.repaint();
                    }
                }
            });
            SwingUtilities.invokeAndWait(Simulator::loadLayout);
        } catch (Throwable e) {
            Log.error(Simulator.class, "Error", e);
            System.exit(-1);
        }
    }

    private static void loadLocale() {
        File layoutFile = new File(netFilePathNoExtension + ".sym_layout");
        if (layoutFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(layoutFile), StandardCharsets.UTF_8))) {
                String content = reader.readLine();
                String[] split = content.split(":");
                if (split[0].equals("locale")) {
                    Locale.setDefault(Locale.of(split[1]));
                }
            } catch (IOException ignore) {
            }
        }
    }
}