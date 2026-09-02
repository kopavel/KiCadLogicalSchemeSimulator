/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
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
import java.util.Map;

import static pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState.none;

public class Transistor implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net currentNet : netFile.getNets().getNet()) {
            Iterator<Node> nodes = currentNet.getNode().iterator();
            while (nodes.hasNext()) {
                Node currentNode = nodes.next();
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
                                    otherNodes.forEach((e, conf) -> {
                                        e.pin = null;
                                        if ("K".equals(conf.pinName)) {
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
                                    //noinspection fallthrough
                                case "K":
                                    schemaPartConfig.params.put(powerState.state ? "openEmitter" : "openCollector", "true");
                                    otherNodes.forEach((e, conf) -> {
                                        e.pin = null;
                                        if ("B".equals(conf.pinName)) {
                                            e.pintype = "input";
                                            e.pinfunction = "IN";
                                        } else {
                                            e.pintype = "output";
                                            e.pinfunction = "OUT";
                                        }
                                    });
                            }
                        }
                        nodes.remove();
                    }
                }
            }
        }
        return result;
    }
}
