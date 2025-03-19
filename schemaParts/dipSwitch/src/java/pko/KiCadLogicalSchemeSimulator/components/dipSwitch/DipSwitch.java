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
package pko.KiCadLogicalSchemeSimulator.components.dipSwitch;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DipSwitch implements NetFilter {
    @Override
    public void doFilter(Export netFile, ParameterResolver parameterResolver) {
        List<Net> forDelete = new ArrayList<>();
        for (Net net : netFile.getNets().getNet()) {
            if (!forDelete.contains(net)) {
                List<Node> forAdd = new ArrayList<>();
                node:
                for (Node node : net.getNode()) {
                    SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(node);
                    if (schemaPartConfig != null && schemaPartConfig.clazz.equals(DipSwitch.class.getSimpleName()) && schemaPartConfig.params.containsKey("On")) {
                        Map<Integer, PinConfig> pinMap = parameterResolver.getPinMap(node);
                        int firstPinNo = Integer.parseInt(node.getPin());
                        int unitNo = pinMap.get(firstPinNo).unitNo;
                        String secondPinNo = String.valueOf(pinMap.entrySet()
                                .stream()
                                .filter(p -> p.getValue().unitNo == unitNo && !p.getKey().equals(firstPinNo))
                                .map(Map.Entry::getKey).findFirst().orElseThrow());
                        for (Net otherNet : netFile.getNets().getNet()) {
                            if (otherNet != net) {
                                if (otherNet.getNode()
                                        .stream()
                                        .anyMatch(n -> n.getRef().equals(node.getRef()) && n.getPin().equals(secondPinNo))) {
                                    forAdd.addAll(otherNet.getNode());
                                    forDelete.add(otherNet);
                                    continue node;
                                }
                            }
                        }
                    }
                }
                net.getNode().addAll(forAdd);
            }
        }
        netFile.getNets().getNet().removeAll(forDelete);
    }
}
