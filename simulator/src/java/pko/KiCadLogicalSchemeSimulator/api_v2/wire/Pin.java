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
package pko.KiCadLogicalSchemeSimulator.api_v2.wire;
import lombok.Getter;
import pko.KiCadLogicalSchemeSimulator.api_v2.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api_v2.schemaPart.SchemaPart;

import java.util.Set;

public abstract class Pin extends ModelItem<Pin> {
    public boolean state;

    @Override
    public Pin copyState(IModelItem<Pin> oldPin) {
        this.strong = oldPin.isStrong();
        this.state = oldPin.getState() > 0;
        this.hiImpedance = oldPin.isHiImpedance();
        return this;
    }

    @Getter
    public boolean strong = true;

    public Pin(Pin oldPin, String variantId) {
        this(oldPin.id, oldPin.parent);
        this.variantId = variantId + (oldPin.variantId == null ? "" : ":" + oldPin.variantId);
        state = oldPin.state;
        strong = oldPin.strong;
        hiImpedance = oldPin.hiImpedance;
    }

    public Pin(String id, SchemaPart parent) {
        super(id, parent);
    }

    abstract public void setState(boolean newState, boolean strong);

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public long getState() {
        return state ? 1L : 0L;
    }

    @Override
    public Pin getOptimised() {
        return this;
    }

    @Override
    public Byte getAliasOffset(String pinName) {
        return 0;
    }

    @Override
    public Set<String> getAliases() {
        return Set.of(id);
    }

    public void resend() {
        if (!hiImpedance) {
            setState(state, strong);
        }
    }

    @Override
    public String toString() {
        return state + ":" + strong + ":" + super.toString();
    }
}
