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
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.InteractiveSchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.net.NetFileParser;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.param.Params;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;
import pko.KiCadLogicalSchemeSimulator.ui.SchemaPartMonitor;
import pko.KiCadLogicalSchemeSimulator.ui.main.MainMenu;
import pko.KiCadLogicalSchemeSimulator.ui.main.MainUI;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static pko.KiCadLogicalSchemeSimulator.api.params.types.RecursionMode.none;
import static pko.KiCadLogicalSchemeSimulator.parsers.symbolMap.SymbolMapFileParser.parse;

@SuppressWarnings({"StaticVariableMayNotBeInitialized", "StaticMethodOnlyUsedInOneClass"})
@CommandLine.Command(name = "", description = "Start Kicad scheme interactive simulation")
public class Simulator implements Runnable {
    private static final Map<String, SchemaPartMonitor> monitoredParts = new HashMap<>();
    private static final Method addReadsMethod;
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("[\r\n]+");
    public static Map<String, SchemaPartSpi> schemaPartSpiMap;
    public static MainUI ui;
    public static String netFilePathNoExtension;
    @CommandLine.Option(names = {"-od", "--optimisedDir"}, description = "Cache directory path for generated optimised classes")
    public static String optimisedDir = "optimised";
    @CommandLine.Option(names = {"-md", "--mapFileDir"}, description = "Map file directory path")
    public static String mapFileDir;
    @CommandLine.Option(names = {"-do", "--disable-optimiser"}, description = "Disable code optimiser")
    public static boolean noOptimiser;
    @CommandLine.Option(names = {"-ro",
            "--recursiveOut"}, description = "Enable recursive event processing for specific part output only, can be specified multiple times, slower simulation")
    public static String[] recursiveOuts;
    public static Net net;
    @CommandLine.Option(names = {"-r", "--recursion"}, description = """
            Recursive event processing mode
            \twarn - warning only (default, little speed overhead, but only detect and report events, can be used for explicitly define pins with recursion and set "disable" method here
            \tall - enable for all pins(slower simulation)
            \tnone - disable for all except explicitly specified (see recursiveOut param)""")
    public static RecursionMode recursionMode;
    static {
        try {
            addReadsMethod = Module.class.getDeclaredMethod("implAddReads", Module.class);
            addReadsMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    @CommandLine.Parameters(index = "0", arity = "1", description = "Path to KiCad NET file")
    public String netFilePath;
    @CommandLine.Option(names = {"-m", "--mapFile"}, description = "Path to KiCad symbol mapping file")
    public String[] mapFiles;

    public static void main(String[] args) {
        new CommandLine(new Simulator()).execute(args);
    }

    public static void addMonitoringPart(String id, Rectangle sizes) {
        SwingUtilities.invokeLater(() -> {
            if (!net.schemaParts.containsKey(id)) {
                Log.warn(Simulator.class, "Unknown monitoring part {} in Layout file", id);
            } else {
                SchemaPartMonitor partMonitor = monitoredParts.computeIfAbsent(id, SchemaPartMonitor::new);
                partMonitor.setVisible(true);
//            partMonitor.toFront();
                if (sizes != null) {
                    partMonitor.setBounds(sizes);
                }
            }
        });
    }

    public static void bringToFront() {
        SwingUtilities.invokeLater(() -> monitoredParts.values().forEach(Window::toFront));
    }

    @SuppressWarnings("deprecation")
    public static void loadLayout() {
        try {
            //FixMe check current screen boundaries
            List<InteractiveSchemaPart> layoutParts = net.schemaParts.values()
                    .stream()
                    .filter(part -> part instanceof InteractiveSchemaPart)
                    .map(part -> (InteractiveSchemaPart) part)
                    .collect(Collectors.toCollection(ArrayList::new));
            File layoutFile = new File(netFilePathNoExtension + ".sym_layout");
            if (layoutFile.isFile()) {
                String content = Utils.readFileToString(layoutFile);
                for (String pos : NEW_LINE_PATTERN.split(content)) {
                    String[] parts = pos.split(":");
                    if ("main".equals(parts[0])) {
                        ui.setBounds(new Rectangle(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])));
                    } else if ("mon".equals(parts[0])) {
                        addMonitoringPart(parts[1],
                                new Rectangle(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5])));
                    } else if (!"locale".equals(parts[0])) {
                        SchemaPart component = net.schemaParts.get(parts[0]);
                        if (component instanceof InteractiveSchemaPart uiComponent) {
                            layoutParts.remove(component);
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
            layoutParts.forEach(part -> {
                AbstractUiComponent g = part.getComponent();
                g.currentX = g.getX();
                g.currentY = g.getY();
                g.reshape(g.currentX, g.currentY, g.getWidth(), g.getHeight());
                g.hasStoredLayout = true;
            });
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
                    .filter(part -> part instanceof InteractiveSchemaPart)
                    .map(part -> part.id + ":" + ((InteractiveSchemaPart) part).getComponent().getX() + ":" + ((InteractiveSchemaPart) part).getComponent().getY())
                    .collect(Collectors.joining("\n"));
            bw.write(content);
            bw.write("\n");
            content = monitoredParts.values()
                    .stream()
                    .map(monitor -> "mon:" + monitor.schemaPart.id + ":" + monitor.getX() + ":" + monitor.getY() + ":" + monitor.getWidth() + ":" +
                            monitor.getHeight())
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
            ParameterResolver parameterResolver = new ParameterResolver();
            if (recursionMode == none) {
                Log.warn(Simulator.class, "Recursive event detection are disabled");
            }
            schemaPartSpiMap = ServiceLoader.load(SchemaPartSpi.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toMap(spi -> spi.getSchemaPartClass().getSimpleName(), spi -> spi));
            Module module = Simulator.class.getModule();
            schemaPartSpiMap.values()
                    .stream()
                    .map(Object::getClass)
                    .map(Class::getModule).forEach(spiModule -> {
                                if (!module.getName().equals(spiModule.getName())) {
                                    try {
                                        addReadsMethod.invoke(module, spiModule);
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            });
            if (!new File(netFilePath).exists()) {
                throw new Exception("Cant fine NET file " + netFilePath);
            }
            netFilePathNoExtension = netFilePath.substring(0, netFilePath.lastIndexOf('.'));
            Params params = null;
            if (new File(netFilePathNoExtension + ".sym_param").exists()) {
                params = XmlParser.parse(netFilePathNoExtension + ".sym_param", Params.class);
                if (params.mapFile != null) {
                    if (mapFiles == null) {
                        mapFiles = params.mapFile.toArray(new String[0]);
                    } else {
                        for (String mapFile : params.mapFile) {
                            mapFiles = Utils.addToArray(mapFiles, mapFile);
                        }
                    }
                }
            }
            if (mapFiles == null) {
                mapFiles = new String[]{"kicad"};
            }
            for (int i = 0; i < mapFiles.length; i++) {
                String mapFile = mapFiles[i];
                if (!mapFile.endsWith(".sym_map")) {
                    mapFile += ".sym_map";
                }
                File file = new File(mapFile);
                if (!file.exists() || !file.isFile()) {
                    if (mapFileDir != null) {
                        file = new File(mapFileDir + File.separator + mapFile);
                        if (!file.exists() || !file.isFile()) {
                            throw new Exception("Can't fine Symbol map file " + mapFile);
                        } else {
                            mapFiles[i] = mapFileDir + "/" + mapFile;
                        }
                    } else {
                        throw new Exception("Can't fine Symbol map file " + mapFile);
                    }
                } else {
                    mapFiles[i] = mapFile;
                }
            }
            for (String mapPath : mapFiles) {
                parse(mapPath, parameterResolver);
            }
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
            if (recursionMode != null) {
                parameterResolver.recursionMode = recursionMode;
            }
            Export export;
            if (netFilePath.endsWith("xml")) {
                export = XmlParser.parse(netFilePath, Export.class);
                export.fillParents();
            } else if (netFilePath.endsWith(".net")) {
                export = NetFileParser.parse(netFilePath);
            } else {
                throw new RuntimeException("Unsupported file extension. " + netFilePath);
            }
            parameterResolver.processNetFile(export, params, recursiveOuts);
            net = new Net(export, optimisedDir, parameterResolver);
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
                if ("locale".equals(split[0])) {
                    Locale.setDefault(Locale.of(split[1]));
                }
            } catch (IOException ignore) {
            }
        }
    }
}