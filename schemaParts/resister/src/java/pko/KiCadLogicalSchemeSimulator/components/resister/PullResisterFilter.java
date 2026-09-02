/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState;
import pko.KiCadLogicalSchemeSimulator.components.power.Power;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.Iterator;

public class PullResisterFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net currentNet : netFile.getNets().getNet()) {
            PowerState powerState = parameterResolver.getPowerState(currentNet);
            if (powerState.state != null) {
                Iterator<Node> nodes = currentNet.getNode().iterator();
                while (nodes.hasNext()) {
                    if (replaceSchemaPart(parameterResolver,
                            nodes.next(),
                            Resister.class,
                            Power.class,
                            powerState.state ? "hi:true" : null,
                            (otherNode, pinConfig) -> {
                                otherNode.setPin(null);
                                otherNode.setPinfunction("OUT");
                                otherNode.setPintype("output");
                            })) {
                        nodes.remove();
                        result = true;
                    }
                }
            }
        }
        return result;
    }
}
