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
package pko.KiCadLogicalSchemeSimulator.components.transistor;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.components.repeater.Repeater;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState.none;

public class Transistor implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net currentNet : netFile.getNets().getNet()) {
            for (Node currentNode : currentNet.getNode()) {
                SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
                if (schemaPartConfig != null && Transistor.class.getSimpleName().equals(schemaPartConfig.clazz)) {
                    PinConfig pinConfig = parameterResolver.getPinMap(currentNode).get(Integer.parseInt(currentNode.getPin()));
                    PowerState powerState = parameterResolver.getPowerState(currentNode);
                    if (powerState != none) {
                        result = true;
                        String pinName = pinConfig.pinName;
                        boolean doCut = false;
                        Map<Node, PinConfig> otherNodes = otherNodes(parameterResolver, currentNode);
                        if (schemaPartConfig.params.containsKey("PNP")) {
                            if (("B".equals(pinName) || "K".equals(pinName)) && powerState.state || "E".equals(pinName) && !powerState.state) {
                                doCut = true;
                            }
                        } else if (("B".equals(pinName) || "K".equals(pinName)) && !powerState.state || "E".equals(pinName) && powerState.state) {
                            doCut = true;
                        }
                        if (doCut) {
                            otherNodes.keySet().forEach(otherNode -> otherNode.parent.node.remove(otherNode));
                        } else {
                            schemaPartConfig.clazz = Repeater.class.getSimpleName();
                            switch (pinName) {
                                case "B":
                                    schemaPartConfig.params.put(powerState.state ? "openCollector" : "openEmitter", "true");
                                    otherNodes.forEach((e, c) -> {
                                        e.pin = null;
                                        if ("K".equals(c.pinName)) {
                                            e.pintype = "output";
                                            e.pinfunction = "OUT";
                                        } else {
                                            e.pintype = "input";
                                            e.pinfunction = "IN";
                                        }
                                    });
                                    break;
                                case "E":
                                    schemaPartConfig.params.put("reverse", "true");
                                case "K":
                                    schemaPartConfig.params.put(powerState.state ? "openEmitter" : "openCollector", "true");
                                    otherNodes.forEach((e, c) -> {
                                        e.pin = null;
                                        if ("B".equals(c.pinName)) {
                                            e.pintype = "input";
                                            e.pinfunction = "IN";
                                        } else {
                                            e.pintype = "output";
                                            e.pinfunction = "OUT";
                                        }
                                    });
                            }
                        }
                        currentNode.parent.node.remove(currentNode);
                    }
                }
            }
        }
        return result;
    }
}
