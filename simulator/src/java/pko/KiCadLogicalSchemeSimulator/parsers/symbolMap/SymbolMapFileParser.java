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
