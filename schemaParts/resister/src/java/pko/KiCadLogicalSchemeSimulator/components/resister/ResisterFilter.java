/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.Objects;

import static pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState.gnd;
import static pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver.PowerState.pwr;

public class ResisterFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        return mergeNets(netFile, parameterResolver, ResisterFilter::doMerge, (_,_) -> true);
    }

    private static Boolean doMerge(ParameterResolver parameterResolver, Node currentNode) {
        SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
        if (schemaPartConfig != null && schemaPartConfig.clazz.equals(Resister.class.getSimpleName())) {
            ParameterResolver.PowerState powerState = parameterResolver.getPowerState(currentNode);
            if (powerState == pwr || powerState == gnd) {
                return false;
            }
            return currentNode.parent.node.stream()
                    .filter(node -> node != currentNode)
                    .filter(node -> !"input".equals(node.pintype))
                    .map(parameterResolver::getSchemaPartConfig)
                    .filter(Objects::nonNull).allMatch(config -> "Power".equals(config.clazz));
        }
        return false;
    }
}
