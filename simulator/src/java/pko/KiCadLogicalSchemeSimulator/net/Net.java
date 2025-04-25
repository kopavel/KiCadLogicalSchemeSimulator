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
package pko.KiCadLogicalSchemeSimulator.net;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.PassivePin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.bus.BusInInterconnect;
import pko.KiCadLogicalSchemeSimulator.net.merger.bus.BusMerger;
import pko.KiCadLogicalSchemeSimulator.net.merger.wire.PassiveInMerger;
import pko.KiCadLogicalSchemeSimulator.net.merger.wire.WireMerger;
import pko.KiCadLogicalSchemeSimulator.net.wire.TriStateNCWire;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.tools.Log;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Net {
    public final Queue<IModelItem<?>> forResend = new LinkedList<>();
    public final Map<String, SchemaPart> schemaParts = new TreeMap<>();
    public final String optimisedDir;
    public final ParameterResolver parameterResolver;
    private final Map<Pin, DestinationWireDescriptor> destinationWireDescriptors = new HashMap<>();
    private final Map<Bus, DestinationBusDescriptor> destinationBusDescriptors = new HashMap<>();
    private final Map<String, BusMerger> busMergers = new TreeMap<>();
    private final Map<String, OutPin> wireMergers = new TreeMap<>();
    private final Map<IModelItem<?>, IModelItem<?>> replacement = new HashMap<>();
    public volatile boolean stabilizing = true;

    public Net(Export export, String optimisedDir, ParameterResolver parameterResolver) throws IOException {
        this.optimisedDir = optimisedDir;
        this.parameterResolver = parameterResolver;
        Log.info(Net.class, "Start Net building");
        boolean doFilter = true;
        while (doFilter) {
            doFilter = false;
            for (NetFilter netFilter : ServiceLoader.load(NetFilter.class)) {
                boolean filterResult = netFilter.doFilter(export, parameterResolver);
                doFilter |= filterResult;
                if (filterResult) {
                    Log.info(Net.class, "Filter " + netFilter.getClass().getSimpleName() + " modify NET");
                }
            }
        }
        export.getNets().getNet().forEach(this::groupSourcesByDestinations);
        buildNet();
        schemaParts.values().forEach(SchemaPart::initOuts);
        Log.info(Net.class, "Stabilizing net");
        stabilise();
        Log.info(Net.class, "Net build complete");
    }

    public Pin processWire(Pin destination, List<OutPin> pins, List<PassivePin> passivePins, Map<OutBus, Long> buses) {
        Pin retVal = null;
        if (buses.size() + pins.size() + passivePins.size() > 1) {
            //connect a destination to multiple sources throe Merger
            String mergerHash = Utils.getHash(pins, passivePins, buses.keySet()) + "masks:" + buses.values()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(":"));
            if (wireMergers.containsKey(mergerHash)) {
                //Use old merger.
                OutPin oldMerger = wireMergers.get(mergerHash);
                oldMerger.addDestination(destination);
                retVal = oldMerger;
            } else {
                //collect merger as wire
                WireMerger newMerger = new WireMerger(destination, pins, passivePins, buses);
                retVal = newMerger;
                wireMergers.put(mergerHash, newMerger);
                if (!passivePins.isEmpty()) {
                    //Bus merger use passive but not regular pins throe wire merger. Index by passive pins either.
                    wireMergers.put(Utils.getHash(passivePins), newMerger);
                }
            }
        } else {
            //No merger needed.
            if (!pins.isEmpty() || !passivePins.isEmpty()) {
                //Pin-to-pin connection. Has only one regular or passive pin.
                for (OutPin pin : pins) {
                    pin.addDestination(destination);
                    retVal = pin;
                }
                for (OutPin pin : passivePins) {
                    pin.addDestination(destination);
                    retVal = pin;
                }
            } else {
                //bus-to-pin connection
                for (Map.Entry<OutBus, Long> bus : buses.entrySet()) {
                    bus.getKey().addDestination(destination, bus.getValue());
                }
            }
        }
        return retVal;
    }

    private void groupSourcesByDestinations(pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net net) {
        if (net.getName().startsWith("unconnected-")) {
            return;
        }
        Map<IModelItem<?>, Byte> sourcesOffset = new HashMap<>();
        List<Pin> destinationPins = new ArrayList<>();
        Map<InBus, SortedSet<Byte>> destinationBusesOffsets = new HashMap<>();
        List<PassivePin> passivePins = new ArrayList<>();
        Boolean powerState = parameterResolver.getPowerState(net);
        net.getNode().forEach(node -> {
            SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(node);
            if (schemaPartConfig == null || schemaPartConfig.ignore) {
                return;
            }
            PinConfig pinConfig = parameterResolver.getPinConfig(node);
            String id = parameterResolver.getId(node);
            SchemaPart schemaPart = this.schemaParts.computeIfAbsent(id, i -> createSchemaPart(schemaPartConfig.clazz, id, schemaPartConfig.getParamString()));
            String pinName = pinConfig == null ? node.getPinfunction() : pinConfig.pinName;
            SchemaPart.PinType pinType = schemaPart.getPinType(pinName);
            if (pinType == null) {
                throw new RuntimeException("Can't determinate pin type for " + id + ":" + pinName);
            }
            switch (pinType) {
                case input -> {
                    IModelItem<?> destination = schemaPart.getInItem(pinName);
                    if (replacement.containsKey(destination)) {
                        destination = replacement.get(destination);
                    }
                    switch (destination) {
                        case InPin pin -> destinationPins.add(pin);
                        case InBus bus -> destinationBusesOffsets.computeIfAbsent(bus, p -> new TreeSet<>()).add(bus.getAliasOffset(pinName));
                        default -> throw new IllegalStateException("Unexpected input type: " + destination.getClass().getName());
                    }
                    if (schemaPartConfig.priority != null && schemaPartConfig.priority.containsKey(destination.getId())) {
                        ((ModelItem<?>) destination).priority += schemaPartConfig.priority.get(destination.getId()) ? 1 : -1;
                    }
                }
                case output -> {
                    if (powerState != null) {
                        throw new AssertionError("OUt pin " + id + "_" + pinName + " on power rail");
                    }
                    IModelItem<?> source = schemaPart.getOutItem(pinName);
                    if (source == null) {
                        throw new AssertionError("No pin named " + pinName + " in schema part " + schemaPart.id);
                    }
                    Byte aliasOffset = source.getAliasOffset(pinName);
                    if (aliasOffset == null) {
                        throw new AssertionError("No alias for pin " + pinName);
                    }
                    if (sourcesOffset.containsKey(source)) {
                        throw new AssertionError("Shortcut on outputs.Part " + source.getName() + " Pin " + sourcesOffset.get(source) + " and " + aliasOffset);
                    }
                    sourcesOffset.put(source, aliasOffset);
                }
                case bidirectional -> {
                    if (powerState != null) {
                        throw new AssertionError("OUt pin on power rail");
                    }
                    IModelItem<?> destination = schemaPart.getInItem(pinName);
                    switch (destination) {
                        case InPin pin -> destinationPins.add(pin);
                        case InBus bus -> destinationBusesOffsets.computeIfAbsent(bus, p -> new TreeSet<>()).add(bus.getAliasOffset(pinName));
                        default -> throw new IllegalStateException("Unexpected input type: " + destination.getClass().getName());
                    }
                    IModelItem<?> source = schemaPart.getOutItem(pinName);
                    assert source != null : "No pin named " + pinName + " in schema part " + schemaPart.id;
                    assert source.getAliasOffset(pinName) != null : "No alias for pin " + pinName;
                    sourcesOffset.put(source, source.getAliasOffset(pinName));
                }
                case passive -> passivePins.add((PassivePin) schemaPart.getOutPin(pinName));
            }
        });
        if (powerState != null) {
            //if on a power rail – connect all pin to individual power out and don't add to any others nets.
            passivePins.forEach(passivePin -> passivePin.merger = new PassiveInMerger(passivePin, powerState));
            passivePins.clear();
        } else {
            //If no destination pins, but has passive (no out only) pins – use NcPin as destination. In other way passive pins don't get any changes.
            if ((destinationPins.isEmpty())) {
                Optional<PassivePin> passivePin = passivePins.stream().findAny();
                if (passivePin.isPresent()) {
                    destinationPins.add(new TriStateNCWire(passivePin.get()));
                } else if (destinationBusesOffsets.isEmpty()) {
                    sourcesOffset.forEach((out, offset) -> Log.warn(Net.class, "Unconnected Out:" + out.getName() + offset));
                }
            }
        }
        //
        //Process Pin destinations
        //
        destinationPins.forEach((destinationPin) -> {
            DestinationWireDescriptor descriptor = destinationWireDescriptors.computeIfAbsent(destinationPin, i -> new DestinationWireDescriptor(passivePins));
            if (TRUE == powerState) {
                SchemaPart pwr = createSchemaPart("Power", "pwr_" + destinationPin.getName(), "hi;strong");
                schemaParts.put(pwr.id, pwr);
                descriptor.add(pwr.getOutItem("OUT"), (byte) 0);
            } else if (FALSE == powerState) {
                SchemaPart gnd = createSchemaPart("Power", "gnd_" + destinationPin.getName(), "strong");
                schemaParts.put(gnd.id, gnd);
                descriptor.add(gnd.getOutItem("OUT"), (byte) 0);
            } else {
                sourcesOffset.forEach(descriptor::add);
            }
        });
        //
        //Process Bus destinations
        //
        destinationBusesOffsets.forEach((destinationBus, destinationOffsets) -> {
            DestinationBusDescriptor descriptor =
                    destinationBusDescriptors.computeIfAbsent((Bus) replacement.getOrDefault(destinationBus, destinationBus), p -> new DestinationBusDescriptor());
            if (TRUE == powerState) {
                for (Byte destinationOffset : destinationOffsets) {
                    SchemaPart pwr = createSchemaPart("Power", "pwr_" + destinationBus.getName(), "hi;strong");
                    schemaParts.put(pwr.id, pwr);
                    descriptor.add(pwr.getOutItem("OUT"), (byte) 0, destinationOffset);
                    sourcesOffset.put(pwr.getOutItem("OUT"), (byte) 0);
                }
            } else if (FALSE == powerState) {
                for (Byte destinationOffset : destinationOffsets) {
                    SchemaPart gnd = createSchemaPart("Power", "gnd_" + destinationBus.getName(), "strong");
                    schemaParts.put(gnd.id, gnd);
                    descriptor.add(gnd.getOutItem("OUT"), (byte) 0, destinationOffset);
                }
            } else {
                if (destinationOffsets.size() > 1) {
                    // interconnected Bus pins
                    destinationBusDescriptors.remove(destinationBus);
                    long interconnectMask = 0;
                    for (Byte offset : destinationOffsets) {
                        interconnectMask |= (1L << offset);
                    }
                    Byte offset = destinationOffsets.getFirst();
                    destinationOffsets.clear();
                    destinationOffsets.add(offset);
                    BusInInterconnect interconnect = new BusInInterconnect(destinationBus, interconnectMask, offset);
                    destinationBusDescriptors.put(interconnect, descriptor);
                    replacement.put(destinationBus, interconnect);
                }
                sourcesOffset.forEach((source, sourceOffset) -> descriptor.add(source, sourceOffset, destinationOffsets.getFirst()));
            }
            passivePins.forEach(passivePin -> descriptor.add(passivePin, (byte) 0, destinationOffsets.getFirst()));
        });
    }

    private void buildNet() {
        //wire destinations
        destinationWireDescriptors.forEach((destination, descriptor) -> processWire(destination, descriptor.pins, descriptor.passivePins, descriptor.buses));
        //Bus destinations
        destinationBusDescriptors.forEach((destination, descriptor) -> {
            descriptor.cleanBuses();
            if (descriptor.useBusMerger()) {
                //Connect a destination to multiple sources throe Merger
                String busMergerHash = descriptor.getHash();
                if (busMergers.containsKey(busMergerHash)) {
                    //just connect to already created bus merger
                    busMergers.get(busMergerHash).addDestination(destination);
                } else {
                    //create new one bus merger
                    BusMerger busMerger = new BusMerger(destination);
                    //add all pins
                    descriptor.offsets.forEach((offset, lists) -> {
                        //search already processed wires
                        String passiveHash = Utils.getHash(lists.passivePins);
                        if (!passiveHash.isBlank() && wireMergers.containsKey(passiveHash)) {
                            //if found by passive pins – connect it and don't process further.
                            busMerger.addSource(wireMergers.get(passiveHash), offset);
                        } else {
                            //process unprocessed wire
                            busMerger.addSource(Net.this, lists.pins, lists.passivePins, offset);
                        }
                    });
                    //add all buses
                    descriptor.buses.forEach((source, offsets) -> offsets.forEach((offset, mask) -> busMerger.addSource(source, mask, offset)));
                    busMergers.put(busMergerHash, busMerger);
                }
            } else {
                //use direct connect to destination
                if (descriptor.buses.isEmpty()) {
                    throw new RuntimeException("Impossible single wire-to-bus connection for destination " + destination.getName());
                } else {
                    //bus-to-bus connection
                    descriptor.buses.forEach((source, offsetMap) -> offsetMap.forEach((offset, mask) -> {
                        source.addDestination(destination, mask, offset);
                    }));
                }
            }
        });
        schemaParts.values()
                .stream()
                .flatMap(p -> p.outPins.values()
                        .stream().distinct().toList()
                        .stream())
                .forEach(this::replaceOut);
        for (OutPin wireMerger : wireMergers.values()) {
            wireMerger.getOptimised(null);
        }
        for (OutBus busMerger : busMergers.values()) {
            busMerger.getOptimised(null);
        }
    }

    private void replaceOut(ModelItem<?> outItem) {
        outItem.getParent().replaceOut(outItem);
    }

    private SchemaPart createSchemaPart(String className, String id, String params) {
        if (!Simulator.schemaPartSpiMap.containsKey(className)) {
            throw new RuntimeException("Unknown SchemaPart class " + className + " for SchemaPart id " + id);
        }
        SchemaPart schemaPart = Simulator.schemaPartSpiMap.get(className).getSchemaPart(id, params);
        if (schemaPart == null) {
            throw new RuntimeException("SchemaPart " + id + " class parameter doesn't reflect AbstractSchemaPart Java class");
        }
        schemaPart.net = this;
        return schemaPart;
    }

    private void stabilise() {
        schemaParts.values()
                .stream()
                .flatMap(p -> p.outPins.values()
                        .stream())
                .filter(i -> !i.isHiImpedance())
                .distinct()
                .forEach(item -> {
                    assert Log.debug(Net.class, "Resend pin {}", item);
                    item.resend();
                    resend();
                });
        wireMergers.values()
                .stream()
                .filter(i -> !i.isHiImpedance()).distinct().forEach(item -> {
                       assert Log.debug(Net.class, "Resend pin {}", item);
                       item.resend();
                       resend();
                   });
        busMergers.values()
                .stream()
                .filter(i -> !i.isHiImpedance()).distinct().forEach(item -> {
                      assert Log.debug(Net.class, "Resend pin {}", item);
                      item.resend();
                      resend();
                  });
        int resendTry = 10;
        for (int i = 0; i < resendTry && !forResend.isEmpty(); i++) {
            resend();
        }
        for (int i = 0; i < resendTry; i++) {
            schemaParts.values().forEach(SchemaPart::reset);
            resend();
        }
        stabilizing = false;
        if (!forResend.isEmpty()) {
            forResend.forEach(item -> {
                try {
                    item.resend();
                } catch (Throwable e) {
                    Log.error(Net.class, "Error at resend {}", item, e);
                }
            });
            Log.error(Net.class, "!!! Can't stabilize Net !!!");
        }
    }

    private void resend() {
        ArrayList<IModelItem<?>> items = new ArrayList<>(forResend);
        forResend.clear();
        items.forEach(item -> {
            assert Log.debug(Net.class, "Resend postponed pin {}", item);
            item.resend();
        });
    }

    private static class BusPinsOffset {
        public final List<OutPin> pins = new ArrayList<>();
        public final List<PassivePin> passivePins = new ArrayList<>();
    }

    public static class DestinationWireDescriptor {
        //Bus, offset
        public final HashMap<OutBus, Long> buses = new HashMap<>();
        //Pin, offset
        public final List<OutPin> pins = new ArrayList<>();
        public final List<PassivePin> passivePins;

        public DestinationWireDescriptor(List<PassivePin> passivePins) {
            this.passivePins = passivePins;
        }

        public void add(IModelItem<?> item, byte offset) {
            switch (item) {
                case OutPin pin -> pins.add(pin);
                case OutBus bus -> buses.put(bus, 1L << offset);
                default -> throw new IllegalStateException("Unsupported item: " + item.getClass().getName());
            }
        }
    }

    private static class DestinationBusDescriptor {
        //Bus, offset, mask
        public final HashMap<OutBus, Map<Byte, Long>> buses = new HashMap<>();
        //Pin, offset
        public final HashMap<Byte, BusPinsOffset> offsets = new HashMap<>();

        public boolean useBusMerger() {
            return buses.values()
                    .stream().mapToLong(Map::size).sum() + offsets.size() > 1;
        }

        public void add(IModelItem<?> item, byte sourceOffset, byte destinationOffset) {
            switch (item) {
                case PassivePin passivePin -> offsets.computeIfAbsent(destinationOffset, e -> new BusPinsOffset()).passivePins.add(passivePin);
                case OutPin pin -> offsets.computeIfAbsent(destinationOffset, e -> new BusPinsOffset()).pins.add(pin);
                case OutBus bus -> {
                    byte offset = (byte) (destinationOffset - sourceOffset);
                    long newMask = buses.computeIfAbsent(bus, p -> new HashMap<>()).computeIfAbsent(offset, p -> 0L) | (1L << sourceOffset);
                    buses.get(bus).put(offset, newMask);
                }
                default -> throw new IllegalStateException("Unsupported item: " + item.getClass().getName());
            }
        }

        //If “offset” has any passive pin – signal need to be processed using wire merger.
        //Drop respective bus mask from bus sources bit, other way got shortcut.
        public void cleanBuses() {
            if (!buses.isEmpty()) {
                offsets.forEach((pinsOffset, lists) -> {
                    if (!lists.passivePins.isEmpty()/*stream().anyMatch(p -> p.source != null)*/) {
                        //clean up all buses mask
                        buses.values()
                                .stream()
                                .flatMap(m -> m.entrySet()
                                        .stream())
                                .filter(o -> o.getKey() <= pinsOffset)
                                .forEach(pair -> {
                                    long correctedMask = ~(1L << (pinsOffset - pair.getKey()));
                                    pair.setValue(pair.getValue() & correctedMask);
                                });
                        //remove empty offsets
                        buses.values().forEach(map -> map.entrySet().removeIf(entry -> entry.getValue() == 0));
                        buses.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                    }
                });
            }
        }

        public String getHash() {
            StringBuilder sb = new StringBuilder();
            offsets.forEach((offset, lists) -> {
                sb.append(offset).append(':');
                lists.passivePins.forEach(p -> sb.append(p.getName()).append(';'));
                lists.pins.forEach(p -> sb.append(p.getName()).append(';'));
            });
            buses.forEach((bus, params) -> {
                sb.append(bus.getName());
                params.forEach((offset, mask) -> sb.append(offset).append(':').append(mask).append(';'));
            });
            return sb.toString();
        }
    }
}
