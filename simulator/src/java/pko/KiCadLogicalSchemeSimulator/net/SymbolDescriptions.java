package pko.KiCadLogicalSchemeSimulator.net;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.*;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SymbolDescriptions {
    public static final Map<String, Map<String, PinMapDescriptor>> schemaPartPinMap = new TreeMap<>();

    public static void parse(String mapPath, SchemaPartMap schemaPartMap) throws IOException {
        if (mapPath != null) {
            SymbolMap symbolMap = XmlParser.parse(mapPath, SymbolMap.class);
            for (Library library : symbolMap.getLib()) {
                SymbolLibMap lib = schemaPartMap.libs.computeIfAbsent(library.getName(), name -> new SymbolLibMap());
                for (Symbol symbol : library.getSymbol()) {
                    SymbolDesc symbolDesc = lib.symbols.computeIfAbsent(symbol.getName(), name -> new SymbolDesc());
                    symbolDesc.clazz = symbol.getSymPartClass();
                    symbolDesc.params = symbol.getSymPartParam();
                    if (symbol.getUnit() != null) {
                        symbolDesc.units = symbol.getUnit()
                                .stream()
                                .map(Unit::getPinMap)
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
            }
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    @AllArgsConstructor
    public static final class PinMapDescriptor {
        public final String pinName;
        public final SchemaPart schemaPart;
    }
}
