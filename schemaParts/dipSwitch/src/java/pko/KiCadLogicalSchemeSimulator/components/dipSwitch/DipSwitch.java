/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.dipSwitch;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

public class DipSwitch implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        return mergeNets(netFile, parameterResolver, DipSwitch::doMerge, (_,_) -> true);
    }

    private static Boolean doMerge(ParameterResolver parameterResolver, Node currentNode) {
        SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
        return schemaPartConfig != null && schemaPartConfig.clazz.equals(DipSwitch.class.getSimpleName()) && schemaPartConfig.params.containsKey("On");
    }
}
