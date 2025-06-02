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
import pko.KiCadLogicalSchemeSimulator.components.led.indicator.LedIndicator;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Export;
import pko.KiCadLogicalSchemeSimulator.parsers.pojo.net.Node;

import java.util.List;
import java.util.Map;

public class ResisterOnLedIndicatorFilter implements NetFilter {
    String otherPinNo;

    @Override
    public boolean doFilter(Export netFile, ParameterResolver parameterResolver) {
        return mergeNets(netFile, parameterResolver, this::doMerge, this::otherPinProvider);
    }

    private Boolean doMerge(ParameterResolver parameterResolver, Node currentNode) {
        SchemaPartConfig schemaPartConfig = parameterResolver.getSchemaPartConfig(currentNode);
        if (schemaPartConfig != null && schemaPartConfig.clazz.equals(Resister.class.getSimpleName())) {
            List<Node> otherNodes = currentNode.parent.node.stream()
                    .filter(node -> node != currentNode).toList();
            if (otherNodes.size() == 1) {
                Node otherNode = otherNodes.getFirst();
                SchemaPartConfig otherPartConfig = parameterResolver.getSchemaPartConfig(otherNode);
                if (otherPartConfig.clazz.equals(LedIndicator.class.getSimpleName())) {
                    Map<Integer, PinConfig> pinMap = parameterResolver.getPinMap(currentNode);
                    int currentPinNo = Integer.parseInt(currentNode.pin);
                    int unitNo = pinMap.get(currentPinNo).unitNo;
                    otherPinNo = String.valueOf(pinMap.entrySet()
                            .stream()
                            .filter(p -> p.getValue().unitNo == unitNo && !p.getKey().equals(currentPinNo))
                            .map(Map.Entry::getKey).findFirst().orElseThrow());
                    return true;
                }
            }
        }
        return false;
    }

    private String otherPinProvider(ParameterResolver parameterResolver, Node currentNode) {
        return otherPinNo;
    }
}
