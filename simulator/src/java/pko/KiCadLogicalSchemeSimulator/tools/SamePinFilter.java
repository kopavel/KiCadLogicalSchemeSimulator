/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;

public class SamePinFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        return mergeNets(netFile, parameterResolver, (_, _) -> true, SamePinFilter::checkPin);
    }

    private static boolean checkPin(PinConfig sourceConfig, PinConfig destinationConfig) {
        return sourceConfig.symbolConfig == destinationConfig.symbolConfig && sourceConfig.pinName.equals(destinationConfig.pinName);
    }
}
