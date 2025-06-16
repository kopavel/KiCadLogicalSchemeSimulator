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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState.none;

public class Transistor implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        Iterator<Net> currentNetIterator = netFile.getNets().getNet().iterator();
        net:
        while (currentNetIterator.hasNext()) {
            Net currentNet = currentNetIterator.next();
            for (Node currentNode : currentNet.getNode()) {
                SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
                if (schemaPartConfig != null && Transistor.class.getSimpleName().equals(schemaPartConfig.clazz)) {
                    PinConfig pinConfig = parameterResolver.getPinMap(currentNode).get(Integer.parseInt(currentNode.getPin()));
                    if ("B".equals(pinConfig != null ? pinConfig.pinName : currentNode.pinfunction)) {
                        Map<Node, PinConfig> otherNodes = otherNodes(parameterResolver, currentNode);
                        List<State> otherPins = otherNodes.entrySet()
                                .stream()
                                .map(entry -> new State(entry.getKey(), entry.getValue(), parameterResolver.getPowerState(entry.getKey()))).toList();
                        if (otherPins.stream()
                                .anyMatch(p -> p.powerState != none)) {
                            result = true;
                            PowerState powerState = none;
                            State targetPin = null;
                            for (State otherPin : otherPins) {
                                if (otherPin.powerState != none) {
                                    otherPin.node.parent.node.remove(otherPin.node);
                                    powerState = otherPin.powerState;
                                } else {
                                    targetPin = otherPin;
                                }
                            }
                            if (targetPin == null) {
                                throw new RuntimeException("Can't identify target pin" + currentNode.ref + ":" + currentNode.pin);
                            }
                            if ("E".equals(targetPin.config.pinName) != powerState.state) {
                                schemaPartConfig.clazz = Repeater.class.getSimpleName();
                                schemaPartConfig.params.put("reverse", "true");
                                targetPin.node.pin = null;
                                targetPin.node.pinfunction = "OUT";
                                targetPin.node.pintype = "output";
                                currentNode.pin = null;
                                currentNode.pinfunction = "IN";
                                currentNode.pintype = "input";
                            } else {
                                List<Node> targetNet = targetPin.node.parent.node;
                                targetNet.addAll(currentNet.node);
                                targetNet.remove(targetPin.node);
                                targetNet.remove(currentNode);
                                currentNetIterator.remove();
                                break net;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private record State(Node node, PinConfig config, PowerState powerState) {
    }
}
