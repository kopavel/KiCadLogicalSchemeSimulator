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
package pko.KiCadLogicalSchemeSimulator.api;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface NetFilter {
    boolean doFilter(Export netFile, ParameterResolver parameterResolver);
    default void modifyOtherNodes(ParameterResolver parameterResolver, Node node, BiConsumer<Node, PinConfig> nodeModifier) {
        Set<Map.Entry<Node, PinConfig>> pins = otherNodes(parameterResolver, node).entrySet();
        if (pins.isEmpty()) {
            throw new RuntimeException("Can't find other nodes, Net:" + node.parent.name + ", Node:" + node.ref + "." + node.pin);
        } else {
            for (Map.Entry<Node, PinConfig> pin : pins) {
                nodeModifier.accept(pin.getKey(), pin.getValue());
            }
        }
    }
    default Map<Node, PinConfig> otherNodes(ParameterResolver parameterResolver, Node node) {
        Map<Node, PinConfig> retVal = new HashMap<>();
        int pinNo = Integer.parseInt(node.getPin());
        Map<Integer, PinConfig> pins = parameterResolver.getPinMap(node);
        PinConfig currentPinConfig = pins.get(pinNo);
        int unitNo = currentPinConfig.unitNo;
        for (Map.Entry<Integer, PinConfig> pin : pins.entrySet()) {
            if (!pin.getKey().equals(pinNo) && pin.getValue().unitNo == unitNo) {
                for (Net otherNet : node.parent.parent.parent.getNets().getNet()) {
                    if (otherNet != node.parent) {
                        for (Node otherNode : otherNet.getNode()) {
                            if (otherNode.getRef().equals(node.getRef()) && pin.getKey().equals(Integer.parseInt(otherNode.pin))) {
                                retVal.put(otherNode, pin.getValue());
                            }
                        }
                    }
                }
            }
        }
        return retVal;
    }
    default boolean replaceSchemaPart(ParameterResolver parameterResolver,
            Node currentNode,
            Class<? extends SchemaPart> oldClass,
            Class<? extends SchemaPart> newClass,
            String sParams,
            BiConsumer<Node, PinConfig> nodeModifier) {
        SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
        if (schemaPartConfig != null && schemaPartConfig.clazz.equals(oldClass.getSimpleName())) {
            schemaPartConfig.clazz = newClass.getSimpleName();
            if (sParams != null && !sParams.isBlank()) {
                Arrays.stream(sParams.split(";")).forEach(param -> {
                    String[] paramPair = param.split(":");
                    if (paramPair.length == 2) {
                        schemaPartConfig.params.put(paramPair[0], paramPair[1]);
                    } else {
                        schemaPartConfig.params.put(paramPair[0], "true");
                    }
                });
            }
            modifyOtherNodes(parameterResolver, currentNode, nodeModifier);
            return true;
        }
        return false;
    }
    default boolean mergeNets(Export netFile,
            ParameterResolver parameterResolver,
            BiFunction<ParameterResolver, Node, Boolean> doMerge,
            Function<PinConfig, Boolean> nodeFilter) {
        boolean retVal = false;
        Iterator<Net> currentNetIterator = netFile.nets.net.iterator();
        nextNet:
        while (currentNetIterator.hasNext()) {
            Net currentNet = currentNetIterator.next();
            for (Node currentNode : currentNet.node) {
                if (doMerge.apply(parameterResolver, currentNode)) {
                    for (Map.Entry<Node, PinConfig> otherNodeConfigs : otherNodes(parameterResolver, currentNode).entrySet()) {
                        if (nodeFilter.apply(otherNodeConfigs.getValue())) {
                            List<Node> otherNode = otherNodeConfigs.getKey().parent.node;
                            otherNode.addAll(currentNet.node);
                            otherNode.remove(currentNode);
                            otherNode.remove(otherNodeConfigs.getKey());
                            currentNet.node.forEach(n -> n.fillParent(otherNodeConfigs.getKey().parent));
                            retVal = true;
                            currentNetIterator.remove();
                            continue nextNet;
                        }
                    }
                }
            }
        }
        return retVal;
    }
}
