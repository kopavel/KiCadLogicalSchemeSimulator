/*
 * Copyright (c) 2024 Pavel Korzh
 * <p>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * <p>
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
 *
 */
package lv.pko.KiCadLogicalSchemeSimulator.model.merger;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.PullPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.TriStateOutPin;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

import java.util.HashMap;
import java.util.Map;

public class Merger extends OutPin {
    private final Map<OutPin, MergerInPin> sources = new HashMap<>();
    public String hash;
    long hiImpedancePins;
    MergerInPin[] inputs = new MergerInPin[0];
    long pullState = 0;
    long nPullMask = -1;
    private long strongMask;
    private long pullMask = 0;

    public Merger(InPin dest) {
        super(dest.id, dest.parent, dest.size);
        for (int i = 0; i < dest.size; i++) {
            dest.mask = (dest.mask << 1) | 1;
        }
        hiImpedancePins = dest.mask;
        dest.addSource(this);
        hash = getHash();
    }

    @Override
    public void addDest(InPin pin) {
        if (dest == null) {
            dest = pin;
        } else if (dest instanceof Splitter splitter) {
            splitter.addDest(pin);
        } else {
            dest = new Splitter(dest, pin);
        }
    }

    public String getHash() {
        StringBuilder result = new StringBuilder(String.valueOf(dest.mask));
        //FixMe do we need ordered list?
        for (MergerInPin input : inputs) {
            result.append(";").append(input.getHash());
        }
        return result.toString();
    }

    public void addSource(OutPin src, long inMask, byte offset) {
        MergerInPin inPin = createInput(src, offset, inMask);
        if (src instanceof PullPin) {
            pullState |= inPin.correctState(src.state) & dest.mask;
            pullMask |= inPin.corrMask;
            nPullMask = ~pullMask;
            state = pullState;
        } else {
            if (!(src instanceof TriStateOutPin)) {
                if ((strongMask & inPin.corrMask) > 0) {
                    throw new RuntimeException("Non tri-state pins overlap on IN pin:" + dest.getName());
                }
                strongMask |= inPin.corrMask;
                hiImpedancePins &= ~strongMask;
            }
            inputs = Utils.addToArray(inputs, inPin);
//            src.addDest(inPin);
            sources.put(src, inPin);
            hash = getHash();
        }
    }

    public void bindSources() {
        for (Map.Entry<OutPin, MergerInPin> bind : sources.entrySet()) {
            bind.getKey().addDest(bind.getValue());
        }
    }

    private MergerInPin createInput(OutPin src, byte offset, long mask) {
        MergerInPin inPin;
        if (offset == 0) {
            inPin = new MergerInPin(src, offset, mask, this);
        } else if (offset > 0) {
            inPin = new PositiveOffsetMergerInPin(src, offset, mask, this);
        } else {
            inPin = new NegativeOffsetMergerInPin(src, offset, mask, this);
        }
        return inPin;
    }
}
