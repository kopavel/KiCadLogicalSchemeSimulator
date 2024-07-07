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
package lv.pko.KiCadLogicalSchemeSimulator;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.PullPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import lv.pko.KiCadLogicalSchemeSimulator.model.InPinInterconnect;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.Merger;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.NCOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.maskGroups.MaskGroupPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.strong.MaskOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.strong.MasksOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.strong.SameMaskOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.triState.MaskTriStateOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.triState.MasksTriStateOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.model.pins.triState.SameMaskTriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.Comp;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.Export;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.Net;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.Property;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.*;
import lv.pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Model {
    public static boolean stabilizing;
    public final Map<String, SchemaPart> schemaParts = new TreeMap<>();
    public final Map<String, Map<String, PinMapDescriptor>> schemaPartPinMap = new TreeMap<>();
    public final Map<String, SchemaPartSpi> schemaPartSpiMap;
    //Simple map from OutPin to InPin
    private final Map<OutPin, Set<InPin>> outMap = new HashMap<>();
    private final Map<InPin, OutPinDescriptor> inMap = new HashMap<>();
    private final Map<String, Merger> mergers = new HashMap<>();

    public Model(Export export, String mapPath) throws IOException {
        Log.info(Model.class, "Start Model building");
        schemaPartSpiMap = ServiceLoader.load(SchemaPartSpi.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(spi -> spi.getSchemaPartClass().getSimpleName(), spi -> spi));
        SchemaPartMap schemaPartMap;
        if (mapPath != null) {
            SymbolMap symbolMap = XmlParser.parse(mapPath, SymbolMap.class);
            schemaPartMap = new SchemaPartMap();
            for (Library library : symbolMap.getLib()) {
                SymbolLibMap lib = schemaPartMap.libs.computeIfAbsent(library.getName(), name -> new SymbolLibMap());
                for (Symbol symbol : library.getSymbol()) {
                    SymbolDesc symbolDesc = lib.symbols.computeIfAbsent(symbol.getName(), name -> new SymbolDesc());
                    symbolDesc.clazz = symbol.getSymPartClass();
                    symbolDesc.params = symbol.getSymPartParam();
                    if (symbol.getUnit() != null) {
                        symbolDesc.units = symbol.getUnit()
                                .stream()
                                .map(Unit::getPinMap)
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
            }
        } else {
            schemaPartMap = null;
        }
        export.getComponents().getComp().forEach((Comp component) -> createSchemaPart(component, schemaPartMap));
        SchemaPart gnd = getSchemaPart("Power", "gnd", "state=0;strong");
        schemaParts.put(gnd.id, gnd);
        SchemaPart pwr = getSchemaPart("Power", "pwr", "state=1;strong");
        schemaParts.put(pwr.id, pwr);
        export.getNets().getNet().forEach(this::processNet);
        buildBuses();
        schemaParts.values().forEach(SchemaPart::initOuts);
        Log.info(Model.class, "Stabilizing model");
        stabilise();
        Log.info(Model.class, "Model build complete");
    }

    @SuppressWarnings("LocalVariableUsedAndDeclaredInDifferentSwitchBranches")
    private void processNet(Net net) {
        Map<OutPin, Byte> outPinsOffset = new HashMap<>();
        Map<InPin, SortedSet<Byte>> inPinsOffsets = new HashMap<>();
        net.getNode().forEach(node -> {
            if ("gnd".equalsIgnoreCase(net.getName())) {
                outPinsOffset.put(schemaParts.get("gnd").getOutPin("OUT"), (byte) 0);
            } else if ("pwr".equalsIgnoreCase(net.getName())) {
                outPinsOffset.put(schemaParts.get("pwr").getOutPin("OUT"), (byte) 0);
            } else {
                Map<String, PinMapDescriptor> pinMap = schemaPartPinMap.get(node.getRef());
                String pinName;
                SchemaPart schemaPart;
                if (pinMap != null) {
                    PinMapDescriptor pinMapDescriptor = pinMap.get(node.getPin());
                    if (pinMapDescriptor == null) {
                        //ignore unmapped pins (power one?)
                        return;
                    }
                    pinName = pinMapDescriptor.pinName;
                    schemaPart = pinMapDescriptor.schemaPart;
                } else {
                    pinName = node.getPinfunction();
                    schemaPart = this.schemaParts.get(node.getRef());
                }
                String pinType = node.getPintype();
                if (pinType.contains("+")) {
                    pinType = pinType.substring(0, pinType.indexOf('+'));
                }
                switch (pinType) {
                    case "input":
                        InPin inPin = schemaPart.getInPin(pinName);
                        inPinsOffsets.computeIfAbsent(inPin, p -> new TreeSet<>()).add(inPin.aliasOffsets.get(pinName));
                        break;
                    case "tri_state":
                    case "output":
                        OutPin outPin = schemaPart.getOutPin(pinName);
                        outPinsOffset.put(outPin, outPin.aliasOffsets.get(pinName));
                        break;
                    case "passive":
                    case "bidirectional":
                        inPin = schemaPart.getInPin(pinName);
                        inPinsOffsets.computeIfAbsent(inPin, p -> new TreeSet<>()).add(inPin.aliasOffsets.get(pinName));
                        outPin = schemaPart.getOutPin(pinName);
                        outPinsOffset.put(outPin, outPin.aliasOffsets.get(pinName));
                        break;
                    case "power_in":
                        //ignore
                        break;
                    default:
                        throw new RuntimeException("Unsupported pin type " + pinType);
                }
            }
        });
        outPinsOffset.forEach((outPin, outPinOffset) -> //
                inPinsOffsets.forEach((inPin, inPinOffsets) -> {
                    if (inPinOffsets.size() > 1) {
                        // interconnected input pins
                        long interconnectMask = 0;
                        for (Byte offset : inPinOffsets) {
                            interconnectMask |= (1L << offset);
                        }
                        InPinInterconnect interconnect = new InPinInterconnect(inPin, interconnectMask);
                        outMap.computeIfAbsent(outPin, p -> new HashSet<>()).add(interconnect);
                        inMap.computeIfAbsent(interconnect, p -> new OutPinDescriptor()).addOutPin(outPin, outPinOffset, inPinOffsets.getFirst());
                    } else {
                        outMap.computeIfAbsent(outPin, p -> new HashSet<>()).add(inPin);
                        inMap.computeIfAbsent(inPin, p -> new OutPinDescriptor()).addOutPin(outPin, outPinOffset, inPinOffsets.getFirst());
                    }
                }));
    }

    private void buildBuses() {
        for (Map.Entry<OutPin, Set<InPin>> outNet : outMap.entrySet()) {
            OutPin outPin = outNet.getKey();
            //todo if ony other out are Power pin - use special OutPin without In splitter
            if (outNet.getValue().size() > 1) {
                //out has many INs - replace instances with appropriate OutPins
                if (outPin instanceof TriStateOutPin triStateOutPin) {
                    replaceOut(outPin, new MasksTriStateOutPins(triStateOutPin));
                } else if (!(outPin instanceof PullPin)) {
                    replaceOut(outPin, new MasksOutPins(outPin));
                }
            }
        }
        inMap.forEach((inPin, outPinDescriptor) -> {
            if (outPinDescriptor.getPermutationCount() == 1) {
                //pin-to-pin connection
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<OutPin, Map<Byte, Long>> outEntry = outPinDescriptor.entrySet()
                        .stream().findFirst().get();
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<Byte, Long> maskEntry = outEntry.getValue().entrySet()
                        .stream().findFirst().get();
                inPin.mask = maskEntry.getValue();
                inPin.addSource(outEntry.getKey());
                inPin.setOffset(maskEntry.getKey());
            } else {
                //connect InPin to multiple OutPins throe Merger
                Merger merger = new Merger(inPin);
                for (Map.Entry<OutPin, Map<Byte, Long>> outPinMap : outPinDescriptor.entrySet()) {
                    for (Map.Entry<Byte, Long> offsetMap : outPinMap.getValue().entrySet()) {
                        merger.addSource(outPinMap.getKey(), offsetMap.getValue(), offsetMap.getKey());
                    }
                }
                //Use only one merger with splitter for a similar in-out connections
                //Hash calculated from destination mask and all sources (OutPins) name/mask/offset
                if (mergers.containsKey(merger.hash)) {
                    mergers.get(merger.hash).addDestination(inPin);
                } else {
                    mergers.put(merger.hash, merger);
                }
            }
        });
        for (Merger merger : mergers.values()) {
            merger.bindSources();
        }
        schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .forEach(outPin -> {
                    if (outPin.noDest()) {
                        replaceOut(outPin, new NCOutPin(outPin));
                    } else if (outPin instanceof MasksTriStateOutPins oldPin && oldPin.groups.length == 1) {
                        if (oldPin.groups[0] instanceof MaskGroupPins) {
                            replaceOut(outPin, new MaskTriStateOutPins(oldPin));
                        } else {
                            replaceOut(outPin, new SameMaskTriStateOutPin(oldPin));
                        }
                    } else if (outPin instanceof MasksOutPins oldPin && oldPin.groups.length == 1) {
                        if (oldPin.groups[0] instanceof MaskGroupPins) {
                            replaceOut(outPin, new MaskOutPins(oldPin));
                        } else {
                            replaceOut(outPin, new SameMaskOutPin(oldPin));
                        }
                    } else if (outPin.getClass() == OutPin.class && outPin.destination.mask == outPin.mask) {
                        replaceOut(outPin, new SameMaskOutPin(outPin));
                    }
                });
    }

    private void replaceOut(OutPin outPin, OutPin newOutPin) {
        outPin.parent.replaceOut(outPin, newOutPin);
        inMap.values().forEach(inNet -> inNet.replaceOutPin(outPin, newOutPin));
    }

    private void createSchemaPart(Comp component, SchemaPartMap map) {
        SymbolDesc symbolDesc = null;
        if (map != null) {
            SymbolLibMap lib = map.libs.get(component.getLibsource().getLib());
            if (lib != null) {
                symbolDesc = lib.symbols.get(component.getLibsource().getPart());
            }
        }
        String id = component.getRef();
        String className = findSchemaPartProperty(component, "SymPartClass");
        if ((className == null || className.isBlank()) && symbolDesc != null) {
            className = symbolDesc.clazz;
        }
        if (className == null || className.isBlank()) {
            throw new RuntimeException("SchemaPart " + id + " has no parameter SymPartClass");
        }
        String parameters = "";
        if (symbolDesc != null && symbolDesc.params != null && !symbolDesc.params.isBlank()) {
            parameters += symbolDesc.params + ";";
        }
        parameters += findSchemaPartProperty(component, "SymPartParam");
        if (symbolDesc == null || symbolDesc.units == null) {
            SchemaPart schemaPart = getSchemaPart(className, id, parameters);
            schemaParts.put(schemaPart.id, schemaPart);
        } else {
            for (int i = 0; i < symbolDesc.units.size(); i++) {
                String unit = symbolDesc.units.get(i);
                String name;
                if (symbolDesc.units.size() == 1) {
                    name = id;
                } else {
                    name = id + "_" + (char) (((byte) 'A') + i);
                }
                SchemaPart schemaPart = getSchemaPart(className, name, parameters);
                schemaParts.put(schemaPart.id, schemaPart);
                for (String pinMapInfo : unit.split(";")) {
                    String[] mapInfo = pinMapInfo.split("=");
                    schemaPartPinMap.computeIfAbsent(id, s -> new HashMap<>()).put(mapInfo[0], new PinMapDescriptor(mapInfo[1], schemaPart));
                }
            }
        }
    }

    private SchemaPart getSchemaPart(String className, String id, String params) {
        if (!schemaPartSpiMap.containsKey(className)) {
            throw new RuntimeException("Unknown SchemaPart class " + className + " for SchemaPart id " + id);
        }
        SchemaPart schemaPart = schemaPartSpiMap.get(className).getSchemaPart(id, params);
        if (schemaPart == null) {
            throw new RuntimeException("SchemaPart " + id + " parameter SymPartClass doesn't reflect AbstractSchemaPart class");
        }
        return schemaPart;
    }

    private String findSchemaPartProperty(Comp component, String name) {
        if (component.getProperty() != null) {
            for (Property property : component.getProperty()) {
                if (property.getName().equals(name)) {
                    return property.getValue();
                }
            }
        }
        return "";
    }

    private void stabilise() {
        stabilizing = true;
        List<OutPin> pins = schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .toList();
        while (stabilizing) {
            stabilizing = false;
            pins.forEach(pin -> {
                try {
                    pin.reSendState();
                } catch (FloatingPinException | ShortcutException e) {
                    stabilizing = true;
                }
            });
            mergers.values().forEach(merger -> {
                try {
                    merger.reSendState();
                } catch (FloatingPinException | ShortcutException e) {
                    stabilizing = true;
                }
            });
            try {
                schemaParts.values().forEach(SchemaPart::reset);
            } catch (FloatingPinException | ShortcutException e) {
                stabilizing = true;
            }
        }
    }

    public record PinMapDescriptor(String pinName, SchemaPart schemaPart) {
    }

    //OutPin, offset, outMask
    public static class OutPinDescriptor extends HashMap<OutPin, Map<Byte, Long>> {
        public long getPermutationCount() {
            return values().stream().mapToLong(Map::size).sum();
        }

        public void addOutPin(OutPin pin, byte outOffset, byte inOffset) {
            byte offset = (byte) (outOffset - inOffset);
            long newMask = computeIfAbsent(pin, p -> new HashMap<>()).computeIfAbsent(offset, p -> 0L) | (1L << outOffset);
            get(pin).put(offset, newMask);
        }

        public void replaceOutPin(OutPin oldPin, OutPin newPin) {
            Map<Byte, Long> pinDescriptor = remove(oldPin);
            if (pinDescriptor != null) {
                put(newPin, pinDescriptor);
            }
        }
    }
}
