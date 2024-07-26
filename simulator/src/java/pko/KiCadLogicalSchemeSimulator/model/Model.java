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
package pko.KiCadLogicalSchemeSimulator.model;
import pko.KiCadLogicalSchemeSimulator.api_v2.*;
import pko.KiCadLogicalSchemeSimulator.api_v2.bus.in.InBus;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.api_v2.wire.in.InPin;
import pko.KiCadLogicalSchemeSimulator.model.bus.BusInInterconnect;
import pko.KiCadLogicalSchemeSimulator.model.merger.IMerger;
import pko.KiCadLogicalSchemeSimulator.model.merger.bus.BusMerger;
import pko.KiCadLogicalSchemeSimulator.model.merger.bus.BusMergerWireIn;
import pko.KiCadLogicalSchemeSimulator.model.merger.wire.WireMerger;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.Comp;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.Property;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.*;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;
import pko.KiCadLogicalSchemeSimulator.tools.Log;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Model {
    public static boolean stabilizing;
    public final Map<String, SchemaPart> schemaParts = new TreeMap<>();
    public final Map<String, Map<String, PinMapDescriptor>> schemaPartPinMap = new TreeMap<>();
    public final Map<String, SchemaPartSpi> schemaPartSpiMap;
    private final Map<ModelInItem, InItemDescriptor> inMap = new HashMap<>();
    private final Map<String, IMerger> mergers = new HashMap<>();

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
        SchemaPart gnd = getSchemaPart("Power", "gnd", "strong");
        schemaParts.put(gnd.id, gnd);
        SchemaPart pwr = getSchemaPart("Power", "pwr", "hi;strong");
        schemaParts.put(pwr.id, pwr);
        export.getNets().getNet().forEach(this::processNet);
        buildBuses();
        schemaParts.values().forEach(SchemaPart::initOuts);
        Log.info(Model.class, "Stabilizing model");
        stabilise();
        Log.info(Model.class, "Model build complete");
    }

    private void processNet(Net net) {
        Map<ModelOutItem, Byte> outsOffset = new HashMap<>();
        Map<ModelInItem, SortedSet<Byte>> insOffsets = new HashMap<>();
        if ("gnd".equalsIgnoreCase(net.getName())) {
            outsOffset.put((ModelOutItem) schemaParts.get("gnd").getOutItem("OUT"), (byte) 0);
        } else if ("pwr".equalsIgnoreCase(net.getName())) {
            outsOffset.put((ModelOutItem) schemaParts.get("pwr").getOutItem("OUT"), (byte) 0);
        }
        net.getNode().forEach(node -> {
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
                case "input" -> {
                    ModelInItem inItem = schemaPart.getInItem(pinName);
                    insOffsets.computeIfAbsent(inItem, p -> new TreeSet<>()).add(inItem.getAliasOffset(pinName));
                }
                case "tri_state", "output" -> {
                    ModelOutItem outItem = (ModelOutItem) schemaPart.getOutItem(pinName);
                    outsOffset.put(outItem, outItem.getAliasOffset(pinName));
                }
                case "bidirectional", "passive" -> {
                    ModelInItem inItem;
                    ModelOutItem outItem;
                    inItem = schemaPart.getInItem(pinName);
                    insOffsets.computeIfAbsent(inItem, p -> new TreeSet<>()).add(inItem.getAliasOffset(pinName));
                    outItem = (ModelOutItem) schemaPart.getOutItem(pinName);
                    outsOffset.put(outItem, outItem.getAliasOffset(pinName));
                }
                case "power_in" -> { //ignore
                }
                default -> throw new RuntimeException("Unsupported pin type " + pinType);
            }
        });
        Map<ModelInItem, ModelInItem> wrappers = new HashMap<>();
        insOffsets.forEach((inItem, inOffsets) -> {
            outsOffset.forEach((outItem, outOffset) -> {
                InItemDescriptor inItemDescriptor = inMap.computeIfAbsent(wrappers.getOrDefault(inItem, inItem), p -> new InItemDescriptor());
                if (inOffsets.size() > 1) {
                    switch (inItem) {
                        case InPin ignored -> throw new RuntimeException("Pin can't be interconnected");
                        case InBus bus -> {
                            // interconnected Bus pins
                            long interconnectMask = 0;
                            for (Byte offset : inOffsets) {
                                interconnectMask |= (1L << offset);
                            }
                            BusInInterconnect interconnect = new BusInInterconnect(bus, interconnectMask, inOffsets.getFirst());
                            inMap.remove(inItem);
                            inMap.put(interconnect, inItemDescriptor);
                            wrappers.put(inItem, interconnect);
                            Byte offset = inOffsets.getFirst();
                            inOffsets.clear();
                            inOffsets.add(offset);
                        }
                        default -> throw new RuntimeException("Unsupported inItem: " + inItem.getClass().getName());
                    }
                }
                inItemDescriptor.addInItem(outItem, outOffset, inOffsets.getFirst());
            });
        });
    }

    private void buildBuses() {
        //FixMe if passive pin are not only one input - remove it (we need it only once in ideal case and it is added as output to mergers)
        //FixMe don't do interconnect throe power rails (seems it's better just generate Power pin for each connection separately)
        inMap.forEach((inItem, inDescriptor) -> {
            if (inDescriptor.getPermutationCount() == 1) {
                //pin-to-pin connection
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<ModelOutItem, Map<Byte, Long>> outItemEntry = inDescriptor.entrySet()
                        .stream().findFirst().get();
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<Byte, Long> maskEntry = outItemEntry.getValue().entrySet()
                        .stream().findFirst().get();
                outItemEntry.getKey().addDestination(inItem, maskEntry.getValue(), maskEntry.getKey());
            } else {
                //connect InPin to multiple OutPins throe Merger
                IMerger merger;
                switch (inItem) {
                    case InBus bus -> merger = new BusMerger(bus);
                    case Pin pin -> merger = new WireMerger(pin);
                    default -> throw new RuntimeException("Unsupported inItem: " + inItem.getClass().getName());
                }
                for (Map.Entry<ModelOutItem, Map<Byte, Long>> outItemEntry : inDescriptor.entrySet()) {
                    for (Map.Entry<Byte, Long> offsetMap : outItemEntry.getValue().entrySet()) {
                        merger.addSource(outItemEntry.getKey(), offsetMap.getValue(), offsetMap.getKey());
                    }
                }
                //Use only one merger with splitter for a similar in-out connections
                //Hash calculated from sources (OutItems) name/mask/offset
                if (merger instanceof BusMerger busMerger) {
                    for (BusMergerWireIn mergerIn : busMerger.wires.values()) {
                        if (mergers.containsKey(mergerIn.input.getHash())) {
                            IMerger oldMerger = mergers.get(mergerIn.input.getHash());
                            oldMerger.addDestination(mergerIn, 0L, (byte) 0);
                            mergerIn.input = (WireMerger) oldMerger;
                        } else {
                            mergers.put(mergerIn.input.getHash(), mergerIn.input);
                        }
                    }
                }
                if (mergers.containsKey(merger.getHash())) {
                    mergers.get(merger.getHash()).addDestination(inItem, 0L, (byte) 0);
                } else {
                    mergers.put(merger.getHash(), merger);
                }
            }
        });
        for (IMerger merger : mergers.values()) {
            merger.bindSources();
        }
        schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .forEach(outItem -> {
                    IModelItem newItem = outItem.getOptimised();
                    if (outItem != newItem) {
                        replaceOut(outItem, newItem);
                    }
                });
    }

    private void replaceOut(IModelItem outItem, IModelItem newOutItem) {
        outItem.getParent().replaceOut(outItem, newOutItem);
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
        List<IModelItem> items = schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .toList();
        while (stabilizing) {
            stabilizing = false;
            items.forEach(item -> {
                try {
                    assert Log.debug(Model.class, "Resend {}", item);
                    item.resend();
                } catch (FloatingInException | ShortcutException e) {
                    assert Log.debug(Model.class, "Item stabilising exception", e);
                    stabilizing = true;
                }
            });
            mergers.values().forEach(merger -> {
                try {
                    assert Log.debug(Model.class, "Resend {}", merger);
                    merger.resend();
                } catch (FloatingInException | ShortcutException e) {
                    assert Log.debug(Model.class, "Merger stabilising exception", e);
                    stabilizing = true;
                }
            });
            try {
                schemaParts.values().forEach(SchemaPart::reset);
            } catch (FloatingInException | ShortcutException e) {
                assert Log.debug(Model.class, "Reset stabilising exception", e);
                stabilizing = true;
            }
        }
    }

    public record PinMapDescriptor(String pinName, SchemaPart schemaPart) {
    }

    //OutPin, offset, outMask
    public static class InItemDescriptor extends HashMap<ModelOutItem, Map<Byte, Long>> {
        public long getPermutationCount() {
            return values().stream().mapToLong(Map::size).sum();
        }

        public void addInItem(ModelOutItem outItem, byte outOffset, byte inOffset) {
            byte offset = (byte) (inOffset - outOffset);
            long newMask = computeIfAbsent(outItem, p -> new HashMap<>()).computeIfAbsent(offset, p -> 0L) | (1L << outOffset);
            get(outItem).put(offset, newMask);
        }
    }
}
