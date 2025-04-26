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
        assertFalse(inPin(id).hiImpedance, "Pin " + id + " hiImpedance should be false");
        assertEquals(state, inPin(id).state, message);
    }

    protected void checkPin(String id, boolean state, String message) {
        assertFalse(inPin(id).hiImpedance, "Pin " + id + " hiImpedance should be false");
        assertEquals(state, inPin(id).state, message);
    }

    protected void checkBus(int id, int state, String message) {
        assertFalse(inBus(id).hiImpedance, "Bus " + id + " hiImpedance should be false");
        assertEquals(state, inBus(id).state, message);
    }

    protected void checkBus(String id, int state, String message) {
        assertFalse(inBus(id).hiImpedance, "Bus " + id + " hiImpedance should be false");
        assertEquals(state, inBus(id).state, message);
    }

    protected void checkBusImpedance(int id, String message) {
        assertTrue(inBus(id).hiImpedance, message);
    }

    protected void checkBusImpedance(String id, String message) {
        assertTrue(inBus(id).hiImpedance, message);
    }

    protected void checkBusImpedance(String partId, String id, String message) {
        assertTrue(net.schemaParts.get(partId).outPins.get(id).isHiImpedance(), message);
    }

    protected void checkPinImpedance(int id, String message) {
        assertTrue(inPin(id).hiImpedance, message);
    }

    protected void checkPinImpedance(String id, String message) {
        assertTrue(inPin(id).hiImpedance, message);
    }

    @BeforeAll
    void loadNet() throws Exception {
        String rootPath = getRootPath();
        Simulator.optimisedDir = rootPath + "/simulator/optimised";
        Simulator.schemaPartSpiMap = ServiceLoader.load(SchemaPartSpi.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toMap(spi -> spi.getSchemaPartClass().getSimpleName(), spi -> spi));
        ParameterResolver parameterResolver = new ParameterResolver();
        Params params = null;
        String netFilePath = getNetFilePath();
        String netFilePathNoExtension = netFilePath.substring(0, netFilePath.lastIndexOf("."));
        String[] mapFiles = new String[]{//
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
