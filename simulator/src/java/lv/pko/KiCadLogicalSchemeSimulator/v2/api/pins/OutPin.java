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
package lv.pko.KiCadLogicalSchemeSimulator.v2.api.pins;
import lv.pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;
import lv.pko.KiCadLogicalSchemeSimulator.v2.model.pins.MaskGroupPins;
import lv.pko.KiCadLogicalSchemeSimulator.v2.model.pins.NCOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.v2.model.pins.OffsetPin;

import java.util.Arrays;

public class OutPin extends Pin {
    public Pin[] destinations;
    public boolean strong;
    public long mask;

    public OutPin(String id, SchemaPart parent, int size, boolean strong, String... names) {
        super(id, parent, size, names);
        this.strong = strong;
        mask = Utils.getMaskForSize(size);
    }

    public OutPin(OutPin oldPin) {
        super(oldPin);
        strong = oldPin.strong;
    }

    public void addDestination(Pin pin, long mask, byte offset) {
        if (offset != 0) {
            pin = new OffsetPin(pin, offset);
        }
        if (mask != this.mask) {
            Arrays.stream(destinations)
                    .filter(d -> d instanceof MaskGroupPins)
                    .map(d -> ((MaskGroupPins) d))
                    .filter(d -> d.mask == mask).findFirst().orElse(new MaskGroupPins(this, mask)).addDestination(pin);
        } else {
            destinations = Utils.addToArray(destinations, pin);
        }
    }

    @Override
    public void setState(long newState, boolean strong) {
        state = newState;
        for (Pin destination : destinations) {
            destination.setState(newState, strong);
        }
    }

    @Override
    public void setHiImpedance() {
        for (Pin destination : destinations) {
            destination.setHiImpedance();
        }
    }

    public void resend() {
        for (Pin destination : destinations) {
            destination.resend(state, strong);
        }
    }

    @Override
    public Pin getOptimised() {
        if (destinations == null) {
            return new NCOutPin(this);
        } else if (destinations.length == 1) {
            return destinations[0].getOptimised();
        } else {
            for (int i = 0; i < destinations.length; i++) {
                destinations[i] = destinations[i].getOptimised();
            }
            return this;
        }
    }
}
