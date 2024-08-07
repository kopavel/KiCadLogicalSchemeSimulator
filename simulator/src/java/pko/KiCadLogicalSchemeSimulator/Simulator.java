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
package pko.KiCadLogicalSchemeSimulator;
import com.formdev.flatlaf.FlatIntelliJLaf;
import picocli.CommandLine;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.net.NetFileParser;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;
import pko.KiCadLogicalSchemeSimulator.ui.main.MainMenu;
import pko.KiCadLogicalSchemeSimulator.ui.main.MainUI;
import pko.KiCadLogicalSchemeSimulator.ui.schemaPartMonitor.SchemaPartMonitor;

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
    public static Net net;
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
                        SchemaPart component = net.schemaParts.get(parts[0]);
                        if (component instanceof InteractiveSchemaPart uiComponent) {
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
        if (net == null) {
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(netFilePathNoExtension + ".sym_layout"))) {
            bw.write("locale:" + Locale.getDefault().getLanguage());
            bw.write("\n");
            bw.write("main:" + ui.getX() + ":" + ui.getY() + ":" + ui.getWidth() + ":" + ui.getHeight());
            bw.write("\n");
            String content = net.schemaParts.values()
                    .stream()
                    .filter(c -> c instanceof InteractiveSchemaPart)
                    .map(c -> c.id + ":" + ((InteractiveSchemaPart) c).getComponent().getX() + ":" + ((InteractiveSchemaPart) c).getComponent().getY())
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
            });
            if (netFilePath.endsWith("xml")) {
                net = new Net(XmlParser.parse(netFilePath, Export.class), mapFile);
            } else if (netFilePath.endsWith(".net")) {
                net = new Net(new NetFileParser().parse(netFilePath), mapFile);
            } else {
                throw new RuntimeException("Unsupported file extension. " + netFilePath);
            }
            net.schemaParts.values().forEach(schemaPart -> {
                if (schemaPart instanceof InteractiveSchemaPart) {
                    try {
                        SwingUtilities.invokeAndWait(() -> ui.add(((InteractiveSchemaPart) schemaPart).getComponent()));
                    } catch (InterruptedException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                ui.setJMenuBar(new MainMenu());
                ui.revalidate();
                ui.repaint();
                for (SchemaPart c : net.schemaParts.values()) {
                    if (c instanceof InteractiveSchemaPart) {
                        AbstractUiComponent component = ((InteractiveSchemaPart) c).getComponent();
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