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
package lv.pko.DigitalNetSimulator.model;
import lv.pko.DigitalNetSimulator.api.chips.Chip;
import lv.pko.DigitalNetSimulator.api.chips.ChipSpi;
import lv.pko.DigitalNetSimulator.api.pins.in.FloatingPinException;
import lv.pko.DigitalNetSimulator.api.pins.in.InPin;
import lv.pko.DigitalNetSimulator.api.pins.out.*;
import lv.pko.DigitalNetSimulator.model.merger.Merger;
import lv.pko.DigitalNetSimulator.parsers.pojo.Comp;
import lv.pko.DigitalNetSimulator.parsers.pojo.Export;
import lv.pko.DigitalNetSimulator.parsers.pojo.Net;
import lv.pko.DigitalNetSimulator.parsers.pojo.Property;
import lv.pko.DigitalNetSimulator.parsers.pojo.symbolMap.Library;
import lv.pko.DigitalNetSimulator.parsers.pojo.symbolMap.Symbol;
import lv.pko.DigitalNetSimulator.parsers.pojo.symbolMap.SymbolMap;
import lv.pko.DigitalNetSimulator.parsers.xml.XmlParser;
import lv.pko.DigitalNetSimulator.tools.Log;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Model {
    public static boolean stabilizing;
    public final Map<String, Chip> chips;
    public final Map<String, ChipSpi> chipsSpiMap;
    private final Map<OutPin, OutPinNet> outMap = new HashMap<>();
    private final Map<InPin, InPinNet> inMap = new HashMap<>();

    public Model(Export export, String mapPath) throws IOException {
        Log.info(Model.class, "Start Model building");
        chipsSpiMap = ServiceLoader.load(ChipSpi.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(spi -> spi.getChipClass().getSimpleName(), spi -> spi));
        ChipMap chipMap;
        if (mapPath != null) {
            SymbolMap symbolMap = XmlParser.parse(mapPath, SymbolMap.class);
            chipMap = new ChipMap();
            for (Library library : symbolMap.getLib()) {
                SymbolLib lib = chipMap.libs.computeIfAbsent(library.getName(), name -> new SymbolLib());
                for (Symbol symbol : library.getSymbol()) {
                    SymbolDesc symbolDesc = lib.symbols.computeIfAbsent(symbol.getName(), name -> new SymbolDesc());
                    symbolDesc.clazz = symbol.getSymPartClass();
                    symbolDesc.params = symbol.getSymPartParam();
                }
            }
        } else {
            chipMap = null;
        }
        chips = export.getComponents().getComp()
                .stream()
                .map((Comp component) -> createSchemaPart(component, chipMap))
                .collect(Collectors.toMap(chip -> chip.id, chip -> chip, (first, second) -> first, TreeMap::new));
        Chip gnd = getSchemaPart("Power", "gnd", "state=0");
        chips.put(gnd.id, gnd);
        Chip pwr = getSchemaPart("Power", "pwr", "state=1");
        chips.put(pwr.id, pwr);
        export.getNets().getNet().forEach(this::processNet);
        buildBuses();
        chips.values().forEach(Chip::initOuts);
        Log.info(Model.class, "Stabilizing model");
        stabilise();
        Log.info(Model.class, "Model build complete");
    }

    @SuppressWarnings("LocalVariableUsedAndDeclaredInDifferentSwitchBranches")
    private void processNet(Net net) {
        Map<OutPin, Byte> outPins = new HashMap<>();
        Map<InPin, Byte> inPins = new HashMap<>();
        net.getNode().forEach(node -> {
            if ("gnd".equals(net.getName())) {
                outPins.put(chips.get("gnd").getOutPin("OUT"), (byte) 1);
            } else if ("pwr".equals(net.getName())) {
                outPins.put(chips.get("pwr").getOutPin("OUT"), (byte) 1);
            }
            String pinName = node.getPinfunction();
            String pinType = node.getPintype();
            if (pinType.contains("+")) {
                pinType = pinType.substring(0, pinType.indexOf('+'));
            }
            Chip chip = chips.get(node.getRef());
            switch (pinType) {
                case "input":
                    InPin inPin = chip.getInPin(pinName);
                    inPins.put(inPin, inPin.aliases.get(pinName));
                    break;
                case "tri_state":
                case "output":
                    OutPin outPin = chip.getOutPin(pinName);
                    outPins.put(outPin, outPin.aliases.get(pinName));
                    break;
/*
                case "passive":
                    outPins.add(chip.getPassivePin(pinName));
                    break;
*/
                case "bidirectional":
                    inPin = chip.getInPin(pinName);
                    inPins.put(inPin, inPin.aliases.get(pinName));
                    outPin = chip.getOutPin(pinName);
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
            for (Map.Entry<InPin, Byte> inPin : inPins.entrySet()) {
                if (!outPin.id.equals(inPin.getKey().id) || !outPin.parent.equals(inPin.getKey().parent)) {
                    outMap.computeIfAbsent(outPin, p -> new OutPinNet()).addInPin(inPin.getKey());
                    inMap.computeIfAbsent(inPin.getKey(), p -> new InPinNet()).addOutPin(outPin, outEntry.getValue(), inPin.getValue());
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
            //todo if ony other out are Power pin - use special OutBus without In splitter
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
                Merger bus = new Merger(inPin);
                for (Map.Entry<OutPin, Map<Byte, Long>> outPinMap : inNet.getValue().outPins.entrySet()) {
                    for (Map.Entry<Byte, Long> offsetMap : outPinMap.getValue().entrySet()) {
                        bus.addSource(outPinMap.getKey(), offsetMap.getValue(), offsetMap.getKey());
                    }
                }
            }
        }
    }

    private Chip getSchemaPart(String className, String id, String params) {
        if (!chipsSpiMap.containsKey(className)) {
            throw new RuntimeException("Unknown SchemaPart class " + className + " for SchemaPart id " + id);
        }
        return chipsSpiMap.get(className).getChip(id, params);
    }

    private Chip createSchemaPart(Comp component, ChipMap map) {
        SymbolDesc symbolDesc = null;
        if (map != null) {
            SymbolLib lib = map.libs.get(component.getLibsource().getLib());
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
        Chip SchemaPart = getSchemaPart(className, id, parameters);
        if (SchemaPart == null) {
            throw new RuntimeException("SchemaPart " + id + " parameter SymPartClass doesn't reflect AbstractSchemaPart class");
        }
        return SchemaPart;
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
        List<OutPin> pins = chips.values()
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

    private static class ChipMap {
        public Map<String, SymbolLib> libs = new HashMap<>();
    }

    private static class SymbolLib {
        public Map<String, SymbolDesc> symbols = new HashMap<>();
    }

    private static class SymbolDesc {
        public String clazz;
        public String params;
    }
}
