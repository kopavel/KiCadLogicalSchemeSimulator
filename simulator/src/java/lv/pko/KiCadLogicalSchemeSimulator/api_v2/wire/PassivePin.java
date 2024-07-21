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
package lv.pko.KiCadLogicalSchemeSimulator.api_v2.wire;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.ModelOutItem;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.bus.Bus;
import lv.pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public abstract class PassivePin extends Pin implements ModelOutItem {
    public Pin[] destinations = new Pin[0];

    public PassivePin(String id, SchemaPart parent) {
        super(id, parent);
    }

    public void addDestination(IModelItem item, long mask, byte offset) {
        switch (item) {
            case Pin pin -> destinations = Utils.addToArray(destinations, pin);
            case Bus bus -> throw new RuntimeException("Can't add bus as destination for PassivePin" + this + "; bus:" + bus);
            default -> throw new RuntimeException("Unsupported destination " + item.getClass().getName());
        }
    }

    @Override
    abstract public void setState(boolean newState, boolean strong);
    @Override
    abstract public void setHiImpedance();

    public void wrap(OutPin source) {
        destinations = source.destinations;
        source.destinations = new Pin[]{this};
    }

    @Override
    abstract public void resend();
}
