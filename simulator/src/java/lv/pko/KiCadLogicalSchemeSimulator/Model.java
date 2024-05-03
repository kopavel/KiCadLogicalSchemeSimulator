/*
 * Copyright (c) 2024 Pavel Korzh
 * <p>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * <p>
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
 *
 */
package lv.pko.KiCadLogicalSchemeSimulator;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.FloatingPinException;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.*;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.nc.NCOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.nc.NCOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.nc.NCTriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.nc.NCTriStateOutPins;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import lv.pko.KiCadLogicalSchemeSimulator.model.InPinInterconnect;
import lv.pko.KiCadLogicalSchemeSimulator.model.InPinNet;
import lv.pko.KiCadLogicalSchemeSimulator.model.OutPinNet;
import lv.pko.KiCadLogicalSchemeSimulator.model.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.model.merger.Merger;
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
    public final Map<String, Map<String, PinMapDescriptor>> shemaPartPinMap = new TreeMap<>();
    public final Map<String, SchemaPartSpi> schemaPartSpiMap;
    private final Map<OutPin, OutPinNet> outMap = new HashMap<>();
    private final Map<InPin, InPinNet> inMap = new HashMap<>();

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
        SchemaPart gnd = getSchemaPart("Power", "gnd", "state=0");
        schemaParts.put(gnd.id, gnd);
        SchemaPart pwr = getSchemaPart("Power", "pwr", "state=1");
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
        Map<OutPin, Byte> outPins = new HashMap<>();
        Map<InPin, List<Byte>> inPins = new HashMap<>();
        net.getNode().forEach(node -> {
            if ("gnd".equals(net.getName())) {
                outPins.put(schemaParts.get("gnd").getOutPin("OUT"), (byte) 1);
            } else if ("pwr".equals(net.getName())) {
                outPins.put(schemaParts.get("pwr").getOutPin("OUT"), (byte) 1);
            }
            Map<String, PinMapDescriptor> pinMap = shemaPartPinMap.get(node.getRef());
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
                    inPins.computeIfAbsent(inPin, p -> new ArrayList<>()).add(inPin.aliases.get(pinName));
                    break;
                case "tri_state":
                case "output":
                    OutPin outPin = schemaPart.getOutPin(pinName);
                    outPins.put(outPin, outPin.aliases.get(pinName));
                    break;
/*
                case "passive":
                    outPins.add(schemaPart.getPassivePin(pinName));
                    break;
*/
                case "bidirectional":
                    inPin = schemaPart.getInPin(pinName);
                    inPins.computeIfAbsent(inPin, p -> new ArrayList<>()).add(inPin.aliases.get(pinName));
                    outPin = schemaPart.getOutPin(pinName);
                    outPins.put(outPin, outPin.aliases.get(pinName));
                    break;
                case "power_in":
                    //just ignore that
                    break;
                default:
                    throw new RuntimeException("Unsupported pin type " + pinType);
            }
        });
        for (Map.Entry<OutPin, Byte> outEntry : outPins.entrySet()) {
            OutPin outPin = outEntry.getKey();
            for (Map.Entry<InPin, List<Byte>> inPin : inPins.entrySet()) {
                if (!outPin.id.equals(inPin.getKey().id) || !outPin.parent.equals(inPin.getKey().parent)) {
                    if (inPin.getValue().size() > 1) {
                        long interMask = 0;
                        for (Byte b : inPin.getValue()) {
                            interMask |= (1L << b);
                        }
                        InPinInterconnect interconnect = new InPinInterconnect(inPin.getKey(), interMask);
                        outMap.computeIfAbsent(outPin, p -> new OutPinNet()).addInPin(interconnect);
                        inMap.computeIfAbsent(interconnect, p -> new InPinNet()).addOutPin(outPin, outEntry.getValue(), inPin.getValue().getFirst());
                    } else {
                        outMap.computeIfAbsent(outPin, p -> new OutPinNet()).addInPin(inPin.getKey());
                        inMap.computeIfAbsent(inPin.getKey(), p -> new InPinNet()).addOutPin(outPin, outEntry.getValue(), inPin.getValue().getFirst());
                    }
                }
            }
        }
    }

    private void replaceOut(OutPin outPin, OutPin newOutPin) {
        outPin.parent.replaceOut(outPin, newOutPin);
        inMap.values().forEach(inNet -> inNet.replaceOutPin(outPin, newOutPin));
    }

    private void buildBuses() {
        for (Map.Entry<OutPin, OutPinNet> outNet : outMap.entrySet()) {
            OutPin outPin = outNet.getKey();
            //todo if ony other out are Power pin - use special OutPin without In splitter
            if (outNet.getValue().inPins.size() > 1) {
                //out has many INs - replace instance with appropriate OutPins
                if (outPin instanceof PullPin pullPin) {
                    replaceOut(outPin, new PullPins(pullPin));
                } else if (outPin instanceof TriStateOutPin triStateOutPin) {
                    replaceOut(outPin, new TriStateOutPins(triStateOutPin));
                } else {
                    replaceOut(outPin, new OutPins(outPin));
                }
            }
        }
        for (Map.Entry<InPin, InPinNet> inNet : inMap.entrySet()) {
            InPin inPin = inNet.getKey();
            if (inNet.getValue().getOutCount() == 1) {
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<OutPin, Map<Byte, Long>> outEntry = inNet.getValue().outPins.entrySet()
                        .stream().findFirst().get();
                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<Byte, Long> maskEntry = outEntry.getValue().entrySet()
                        .stream().findFirst().get();
                inPin.addSource(outEntry.getKey());
                if (maskEntry.getKey() > 0) {
                    inPin.setOffset(maskEntry.getKey());
                }
                inPin.mask = maskEntry.getValue();
            } else {
                Merger merger = new Merger(inPin);
                for (Map.Entry<OutPin, Map<Byte, Long>> outPinMap : inNet.getValue().outPins.entrySet()) {
                    for (Map.Entry<Byte, Long> offsetMap : outPinMap.getValue().entrySet()) {
                        merger.addSource(outPinMap.getKey(), offsetMap.getValue(), offsetMap.getKey());
                    }
                }
            }
        }
        schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .forEach(outPin -> {
                    if (outPin.noDest()) {
                        replaceOut(outPin, switch (outPin) {
                            case OutPins pin -> new NCOutPins(pin);
                            case TriStateOutPins pin -> new NCTriStateOutPins(pin);
                            case TriStateOutPin pin -> new NCTriStateOutPin(pin);
                            case OutPin pin -> new NCOutPin(pin);
                        });
                    }
                });
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
        if (symbolDesc != null) {
            parameters += symbolDesc.params + ";";
        }
        parameters += findSchemaPartProperty(component, "SymPartParam");
        if (symbolDesc == null || symbolDesc.units == null) {
            SchemaPart schemaPart = getSchemaPart(className, id, parameters);
            schemaParts.put(schemaPart.id, schemaPart);
        } else {
            for (int i = 0; i < symbolDesc.units.size(); i++) {
                String unit = symbolDesc.units.get(i);
                SchemaPart schemaPart = getSchemaPart(className, id + "_" + (char) (((byte) 'A') + i), parameters);
                schemaParts.put(schemaPart.id, schemaPart);
                for (String pinMapInfo : unit.split(";")) {
                    String[] mapInfo = pinMapInfo.split("=");
                    shemaPartPinMap.computeIfAbsent(id, s -> new HashMap<>()).put(mapInfo[0], new PinMapDescriptor(mapInfo[1], schemaPart));
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
        }
    }

    public record PinMapDescriptor(String pinName, SchemaPart schemaPart) {
    }
}
