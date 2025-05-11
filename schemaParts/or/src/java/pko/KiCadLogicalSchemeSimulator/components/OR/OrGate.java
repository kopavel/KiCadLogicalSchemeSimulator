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
package pko.KiCadLogicalSchemeSimulator.components.OR;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

import java.util.HashMap;
import java.util.Map;

public class OrGate extends SchemaPart {
    private final Map<String, OrGateIn> ins = new HashMap<>();
    public Pin out;
    public int inState;

    public OrGate(String id, String sParam) {
        super(id, sParam);
        if (params.containsKey("openCollector")){
            addTriStateOutPin("OUT", false);
        } else {
            addOutPin("OUT", false);
        }
        if (!params.containsKey("size")) {
            throw new RuntimeException("Component " + id + " has no parameter \"size\"");
        }
        int pinAmount = Integer.parseInt(params.get("size"));
        for (int i = 0; i < pinAmount; i++) {
            int mask = 1 << i;
            ins.put("IN" + i, addInPin(new OrGateIn("IN" + i, this, mask)));
        }
    }

    @Override
    public void initOuts() {
        out = getOutPin("OUT");
        ins.values().forEach(pin -> {
            if (!pin.isHiImpedance() && pin.state) {
                inState |= (1 << Integer.parseInt(pin.getId().substring(2)));
            }
            pin.out = out;
        });
        out.state = inState != 0 ? nReverse : reverse;
    }

    @Override
    public <T> void replaceIn(ModelItem<T> oldIn, ModelItem<T> newIn) {
        super.replaceIn(oldIn, newIn);
        ins.put(oldIn.getId(), (OrGateIn) newIn);
    }

    @Override
    public String extraState() {
        return reverse ? "reverse" : null;
    }
}
