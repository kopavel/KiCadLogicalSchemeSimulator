/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.test.schemaPartTester;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;
import pko.KiCadLogicalSchemeSimulator.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.net.NetFileParser;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.param.Params;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;
import pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.io.File;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pko.KiCadLogicalSchemeSimulator.parsers.symbolMap.SymbolMapFileParser.parse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class NetTester {
    protected Net net;

    protected abstract String getNetFilePath();
    protected abstract String getRootPath();

    protected InPin inPin(int id) {
        return (InPin) net.schemaParts.get("InPin" + id).inPins.get("In");
    }

    protected InBus inBus(int id) {
        return (InBus) net.schemaParts.get("InBus" + id).inPins.get("In");
    }

    protected Bus outBus(int id) {
        return (Bus) net.schemaParts.get("OutBus" + id).outPins.get("Out");
    }

    protected Pin outPin(int id) {
        return (Pin) net.schemaParts.get("OutPin" + id).outPins.get("Out");
    }

    protected InPin inPin(String id) {
        if (net.schemaParts.containsKey(id)) {
            return (InPin) net.schemaParts.get(id).inPins.get("In");
        } else {
            return (InPin) net.schemaParts.get(id + "1").inPins.get("In");
        }
    }

    protected InBus inBus(String id) {
        return (InBus) net.schemaParts.get(id + "1").inPins.get("In");
    }

    protected Bus outBus(String id) {
        return (Bus) net.schemaParts.get(id + "1").outPins.get("Out");
    }

    protected Pin outPin(String id) {
        if (net.schemaParts.containsKey(id)) {
            return (Pin) net.schemaParts.get(id).outPins.get("Out");
        } else {
            return (Pin) net.schemaParts.get(id + "1").outPins.get("Out");
        }
    }

    protected void setBus(String id, int state) {
        outBus(id).setState(state);
    }

    protected void setBus(int id, int state) {
        outBus(id).setState(state);
    }

    protected void setLo(String id) {
        outPin(id).setLo();
    }

    protected void setHi(String id) {
        outPin(id).setHi();
    }

    protected void setHi(int id) {
        outPin(id).setHi();
    }

    protected void setLo(int id) {
        outPin(id).setLo();
    }

    protected void checkPin(int id, boolean state, String message) {
        assertFalse(inPin(id).isHiImpedance(), "Pin " + id + " hiImpedance should be false");
        assertEquals(state, inPin(id).state, message);
    }

    protected void checkPin(String id, boolean state, String message) {
        assertFalse(inPin(id).isHiImpedance(), "Pin " + id + " hiImpedance should be false");
        assertEquals(state, inPin(id).state, message);
    }

    protected void checkBus(int id, int state, String message) {
        assertFalse(inBus(id).isHiImpedance(), "Bus " + id + " hiImpedance should be false");
        assertEquals(state, inBus(id).state, message);
    }

    protected void checkBus(String id, int state, String message) {
        assertFalse(inBus(id).isHiImpedance(), "Bus " + id + " hiImpedance should be false");
        assertEquals(state, inBus(id).state, message);
    }

    protected void checkBusImpedance(int id, String message) {
        assertTrue(inBus(id).isHiImpedance(), message);
    }

    protected void checkBusImpedance(String id, String message) {
        assertTrue(inBus(id).isHiImpedance(), message);
    }

    protected void checkBusImpedance(String partId, String id, String message) {
        assertTrue(net.schemaParts.get(partId).outPins.get(id).isHiImpedance(), message);
    }

    protected void checkPinImpedance(int id, String message) {
        assertTrue(inPin(id).isHiImpedance(), message);
    }

    protected void checkPinImpedance(String id, String message) {
        assertTrue(inPin(id).isHiImpedance(), message);
    }

    @BeforeAll
    protected void loadNet() throws Exception {
        String rootPath = getRootPath();
        Simulator.optimisedDir = rootPath + "/simulator/optimised";
        Simulator.schemaPartSpiMap = ServiceLoader.load(SchemaPartSpi.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(spi -> spi.getSchemaPartClass().getSimpleName(), spi -> spi));
        ParameterResolver parameterResolver = new ParameterResolver();
        Params params = null;
        String netFilePath = getNetFilePath();
        String netFilePathNoExtension = netFilePath.substring(0, netFilePath.lastIndexOf('.'));
        String[] mapFiles = {//
                rootPath + "/stuff/kicad_symbols/kicad.sym_map",//
                rootPath + "/stuff/kicad_symbols/chip.sym_map",//
                rootPath + "/stuff/kicad_symbols/test.sym_map"//
        };
        if (new File(netFilePathNoExtension + ".sym_param").exists()) {
            params = XmlParser.parse(netFilePathNoExtension + ".sym_param", Params.class);
            if (params.mapFile != null) {
                for (String mapFile : params.mapFile) {
                    mapFiles = Utils.addToArray(mapFiles, mapFile);
                }
            }
        }
        for (String mapPath : mapFiles) {
            parse(mapPath, parameterResolver);
        }
        Export export = NetFileParser.parse(getNetFilePath());
        parameterResolver.processNetFile(export, params, null);
        net = new Net(export, rootPath + "/simulator/optimised", parameterResolver);
    }
}
