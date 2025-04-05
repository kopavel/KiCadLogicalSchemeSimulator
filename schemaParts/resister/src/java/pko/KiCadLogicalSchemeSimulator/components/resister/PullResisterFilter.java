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
package pko.KiCadLogicalSchemeSimulator.components.resister;
import pko.KiCadLogicalSchemeSimulator.api.NetFilter;
import pko.KiCadLogicalSchemeSimulator.api.params.ParameterResolver;
import pko.KiCadLogicalSchemeSimulator.api.params.types.PinConfig;
import pko.KiCadLogicalSchemeSimulator.api.params.types.SchemaPartConfig;
import pko.KiCadLogicalSchemeSimulator.components.power.Power;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Net;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.Iterator;
import java.util.Map;

public class PullResisterFilter implements NetFilter {
    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        boolean result = false;
        for (Net net : netFile.getNets().getNet()) {
            Boolean powerState = parameterResolver.getPowerState(net);
            if (powerState != null) {
                node:
                for (Iterator<Node> iterator = net.getNode().iterator(); iterator.hasNext(); ) {
                    Node node = iterator.next();
                    SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(node);
                    if (schemaPartConfig != null && schemaPartConfig.clazz.equals(Resister.class.getSimpleName())) {
                        schemaPartConfig.clazz = Power.class.getSimpleName();
                        if (powerState) {
                            schemaPartConfig.params.put("hi", "true");
                        }
                        Map<Integer, PinConfig> pinMap = parameterResolver.getPinMap(node);
                        for (Integer pin : pinMap.keySet()) {
                            if (!pin.equals(Integer.parseInt(node.getPin()))) {
                                for (Net otherNet : netFile.getNets().getNet()) {
                                    if (otherNet == net) {
                                        continue;
                                    }
                                    for (Node otherNode : otherNet.getNode()) {
                                        if (otherNode.getRef().equals(node.getRef())) {
                                            otherNode.setPin(null);
                                            otherNode.setPinfunction("OUT");
                                            otherNode.setPintype("output");
                                            iterator.remove();
                                            result = true;
                                            continue node;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
