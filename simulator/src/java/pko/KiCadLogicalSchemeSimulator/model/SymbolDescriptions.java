package pko.KiCadLogicalSchemeSimulator.model;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.symbolMap.*;
import pko.KiCadLogicalSchemeSimulator.parsers.xml.XmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SymbolDescriptions {
    public static final Map<String, Map<String, PinMapDescriptor>> schemaPartPinMap = new TreeMap<>();

    public static SchemaPartMap parse(String mapPath) throws IOException {
        if (mapPath != null) {
            SymbolMap symbolMap = XmlParser.parse(mapPath, SymbolMap.class);
            SchemaPartMap schemaPartMap = new SchemaPartMap();
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
            return schemaPartMap;
        }
        return null;
    }

    @SuppressWarnings("ClassCanBeRecord")
    @AllArgsConstructor
    public static final class PinMapDescriptor {
        public final String pinName;
        public final SchemaPart schemaPart;
    }
}
