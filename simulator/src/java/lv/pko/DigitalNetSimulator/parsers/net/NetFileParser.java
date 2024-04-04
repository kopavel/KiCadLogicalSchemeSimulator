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
package lv.pko.DigitalNetSimulator.parsers.net;
import lv.pko.DigitalNetSimulator.parsers.pojo.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetFileParser {
    private int currentChar;

    public Export parse(String filePath) throws Exception {
        Export export = new Export();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            NetFileItem netFile = parse(reader);
            //create components
            Components components = new Components();
            components.setComp(new ArrayList<>());
            export.setComponents(components);
            Nets nets = new Nets();
            nets.setNet(new ArrayList<>());
            export.setNets(nets);
            netFile.items.get("components").getFirst().items.get("comp").forEach(node -> {
                Comp comp = new Comp();
                comp.setProperty(new ArrayList<>());
                export.getComponents().getComp().add(comp);
                comp.setRef(node.items.get("ref").getFirst().value);
                LibSource libSource = new LibSource();
                comp.setLibsource(libSource);
                NetFileItem nodeLibSource = node.items.get("libsource").getFirst();
                libSource.setLib(nodeLibSource.items.get("lib").getFirst().value);
                libSource.setPart(nodeLibSource.items.get("part").getFirst().value);
                addSchemaPartProperty(node, "SymPartClass", comp.getProperty());
                addSchemaPartProperty(node, "SymPartParam", comp.getProperty());
            });
            //create wires
            netFile.items.get("nets").getFirst().items.get("net").forEach(netNode -> {
                Net net = new Net();
                export.getNets().getNet().add(net);
                net.setNode(new ArrayList<>());
                net.setName(netNode.items.get("name").getFirst().value);
                netNode.items.get("node").forEach(nodeItem -> {
                    Node node = new Node();
                    net.getNode().add(node);
                    node.setRef(nodeItem.items.get("ref").getFirst().value);
                    node.setPinfunction(nodeItem.items.get("pinfunction").getFirst().value);
                    node.setPintype(nodeItem.items.get("pintype").getFirst().value);
                });
            });
            return export;
        } catch (IOException e) {
            throw new Exception("Can't open input file " + filePath, e);
        }
    }

    private NetFileItem parse(BufferedReader reader) throws Exception {
        currentChar = reader.read(); //just skip first parenthesis
        if (currentChar == '(') {
            return getItem(reader);
        }
        throw new Exception("Can't parse NET file");
    }

    private NetFileItem getItem(BufferedReader reader) throws Exception {
        NetFileItem retVal = new NetFileItem();
        nextChar(reader);
        //read name
        StringBuilder name = new StringBuilder();
        while (currentChar >= 0 && currentChar != ' ' && currentChar != ')') {
            name.append((char) currentChar);
            nextChar(reader);
        }
        retVal.name = name.toString();
        //search value start
        if (currentChar != ')') {
            nextNoSpaceChar(reader);
        }
        while (currentChar > 0 && currentChar != ')') {
            if (currentChar == '\"') {
                retVal.value = getString(reader);
            } else if (currentChar == '(') {
                NetFileItem child = getItem(reader);
                retVal.items.computeIfAbsent(child.name, k -> new ArrayList<>()).add(child);
                nextNoSpaceChar(reader);
            } else {
                throw new Exception("Unexpected char:" + (char) currentChar);
            }
        }
        if (currentChar == -1) {
            throw new Exception("Unexpected end of file");
        }
        return retVal;
    }

    private void nextNoSpaceChar(BufferedReader reader) throws IOException {
        nextChar(reader);
        while (currentChar == ' ' || currentChar == '\r' || currentChar == '\n') {
            nextChar(reader);
        }
    }

    private void nextChar(BufferedReader reader) throws IOException {
        currentChar = reader.read();
        while (currentChar == '\r' || currentChar == '\n') {
            currentChar = reader.read();
        }
    }

    private String getString(BufferedReader reader) throws IOException {
        StringBuilder string = new StringBuilder();
        currentChar = reader.read();
        boolean escape = false;
        while (currentChar >= 0 && (currentChar != '"' || escape)) {
            if (currentChar == '\\' && !escape) {
                escape = true;
            } else {
                string.append((char) currentChar);
                escape = false;
            }
            currentChar = reader.read();
        }
        if (currentChar == -1) {
            throw new RuntimeException("Unexpected end of file");
        }
        currentChar = reader.read();//just skip closing "
        return string.toString();
    }

    private void addSchemaPartProperty(NetFileItem node, String name, List<Property> propertyList) {
        Property prop = new Property().setName(name);
        for (NetFileItem property : node.items.get("property")) {
            if (property.items.get("name").getFirst().value.equals(name)) {
                propertyList.add(prop.setValue(property.items.get("value").getFirst().value));
            }
        }
    }
}
