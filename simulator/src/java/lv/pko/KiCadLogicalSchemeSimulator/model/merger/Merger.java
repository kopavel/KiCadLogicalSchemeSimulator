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
import lv.pko.KiCadLogicalSchemeSimulator.model.ShortcutException;
import lv.pko.KiCadLogicalSchemeSimulator.tools.Utils;

public class Merger extends OutPin {
    private NoOffsetMergerInPin[] inputs = new NoOffsetMergerInPin[0];
    private long strongMask;
    private long pullState = 0;
    private long pullMask = 0;

    public Merger(InPin dest) {
        super(dest.id, dest.parent, dest.size);
        for (int i = 0; i < dest.size; i++) {
            dest.mask = (dest.mask << 1) | 1;
        }
        dest.addSource(this);
    }

    public void addSource(OutPin src, long inMask, byte offset) {
        if (!(src instanceof TriStateOutPin) && !(src instanceof PullPin)) {
            long newMask;
            if (offset < 0) {
                newMask = inMask << -offset;
            } else {
                newMask = inMask >> offset;
            }
            if ((strongMask & newMask) > 0) {
                throw new RuntimeException("Non tri-state pins overlap on IN pin:" + dest.getName());
            }
            strongMask |= newMask;
        }
        NoOffsetMergerInPin inPin;
        inPin = createInput(src, offset, inMask);
        if (src instanceof PullPin) {
            pullState |= inPin.correctState(src.state);
            pullMask |= inPin.corrMask;
        } else {
            inputs = Utils.addToArray(inputs, inPin);
        }
        src.addDest(inPin);
    }

    private NoOffsetMergerInPin createInput(OutPin src, byte offset, long mask) {
        NoOffsetMergerInPin inPin;
        if (offset == 0) {
            inPin = new NoOffsetMergerInPin(src, offset, mask) {
                @Override
                public void onMerge() {
                    long strongPins = 0;
                    long pullPins = 0;
                    long result = 0;
                    for (NoOffsetMergerInPin input : inputs) {
                        if (!input.hiImpedance) {
                            if ((strongPins & input.corrMask) > 0) {
                                throw new ShortcutException(inputs);
                            }
                            strongPins |= input.corrMask;
                            result |= input.rawState;
                        } else {
                            pullPins |= pullMask & input.corrMask;
                            result |= pullState & input.corrMask;
                        }
                    }
                    dest.rawState = result;
                    state = result;
                    //FixMe what about edge pins??
                    dest.onChange(result, (strongPins | pullPins) != dest.mask);
                }
            };
        } else if (offset > 0) {
            inPin = new PositiveOffsetMergerInPin(src, offset, mask) {
                @Override
                public void onMerge() {
                    long strongPins = 0;
                    long pullPins = 0;
                    long result = 0;
                    for (NoOffsetMergerInPin input : inputs) {
                        if (!input.hiImpedance) {
                            if ((strongPins & input.corrMask) > 0) {
                                throw new ShortcutException(inputs);
                            }
                            strongPins |= input.corrMask;
                            result |= input.rawState;
                        } else {
                            pullPins |= pullMask & input.corrMask;
                            result |= pullState & input.corrMask;
                        }
                    }
                    dest.rawState = result;
                    state = result;
                    //FixMe what about edge pins??
                    dest.onChange(result, (strongPins | pullPins) != dest.mask);
                }
            };
        } else {
            inPin = new NegativeOffsetMergerInPin(src, offset, mask) {
                @Override
                public void onMerge() {
                    long strongPins = 0;
                    long pullPins = 0;
                    long result = 0;
                    for (NoOffsetMergerInPin input : inputs) {
                        if (!input.hiImpedance) {
                            if ((strongPins & input.corrMask) > 0) {
                                throw new ShortcutException(inputs);
                            }
                            strongPins |= input.corrMask;
                            result |= input.rawState;
                        } else {
                            pullPins |= pullMask & input.corrMask;
                            result |= pullState & input.corrMask;
                        }
                    }
                    dest.rawState = result;
                    state = result;
                    //FixMe what about edge pins??
                    dest.onChange(result, (strongPins | pullPins) != dest.mask);
                }
            };
        }
        return inPin;
    }
}
