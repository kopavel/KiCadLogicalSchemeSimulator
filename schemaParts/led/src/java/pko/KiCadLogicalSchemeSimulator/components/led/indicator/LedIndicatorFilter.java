/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led.indicator;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.components.led.Led;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.Iterator;

public class LedIndicatorFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net currentNet : netFile.getNets().getNet()) {
            ParameterResolver.PowerState powerState = parameterResolver.getPowerState(currentNet);
            if (powerState.state != null) {
                Iterator<Node> nodes = currentNet.getNode().iterator();
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(node);
                    if (schemaPartConfig!=null && Led.class.getSimpleName().equals(schemaPartConfig.clazz)) {
                        PinConfig pinConfig = parameterResolver.getPinMap(node).get(Integer.parseInt(node.getPin()));
                        boolean anode = "A".equals(pinConfig != null ? pinConfig.pinName : node.pinfunction);
                        if (powerState.state && anode || !powerState.state && !anode) {
                            if (replaceSchemaPart(parameterResolver, node, Led.class, LedIndicator.class, anode ? "reverse" : null, (otherNode, pc) -> {
                                otherNode.setPin(null);
                                otherNode.setPinfunction("IN");
                                otherNode.setPintype("input");
                            })) {
                                nodes.remove();
                                result = true;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
