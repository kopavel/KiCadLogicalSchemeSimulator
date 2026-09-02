/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.parsers.symbolMap;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SymbolConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.Library;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.Symbol;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.SymbolMap;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.Unit;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;

import java.io.IOException;

public enum SymbolMapFileParser {
    ;

    public static void parse(String mapPath, ParameterResolver parameterResolver) throws IOException {
        if (mapPath != null) {
            SymbolMap xmlSymbolMap = XmlParser.parse(mapPath, SymbolMap.class);
            for (Library library : xmlSymbolMap.getLib()) {
                for (Symbol symbol : library.getSymbol()) {
                    SymbolConfig symbolConfig = parameterResolver.addSymbol(library.getName(), symbol.getName(), symbol.getClazz(), symbol.getParam());
                    if (symbol.getUnit() != null) {
                        symbol.getUnit()
                                .stream()
                                .map(Unit::getPinMap).forEachOrdered(symbolConfig::addUnit);
                    }
                }
            }
        }
    }
}
