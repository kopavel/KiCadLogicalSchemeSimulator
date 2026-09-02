/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.mos6532;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.components.power.Power;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class M6532PullUpFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net currentNet : netFile.getNets().getNet()) {
            Iterator<Node> nodes = currentNet.getNode().iterator();
            Collection<Node> newNodes = new ArrayList<>();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(node);
                if (schemaPartConfig != null && Mos6532.class.getSimpleName().equals(schemaPartConfig.clazz)) {
                    PinConfig pinConfig = parameterResolver.getPinMap(node).get(Integer.parseInt(node.getPin()));
                    String pinName = (pinConfig != null ? pinConfig.pinName : node.pinfunction);
                    if (pinName.startsWith("PA") || pinName.startsWith("PB")) {
                        PowerState powerState = parameterResolver.getPowerState(currentNet);
                        if (powerState == PowerState.none) {
                            String newId = parameterResolver.getId(node) + pinName + "pullup";
                            addPart(parameterResolver,newId, Power.class.getSimpleName(), "hi");
                            Node newNode = new Node();
                            newNode.setRef(newId);
                            newNode.setPintype("output");
                            newNode.setPinfunction("OUT");
                            newNode.parent=currentNet;
                            newNodes.add(newNode);
                        }
                    }
                }
            }
            if (!newNodes.isEmpty()) {
                currentNet.getNode().addAll(newNodes);
                result = true;
            }
        }
        return result;
    }


}
